package unichope.platform.core.postgres;

import static org.junit.jupiter.api.Assertions.*;
import static unichope.platform.core.LifecycleFailure.Code.*;

import java.sql.Connection;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import unichope.platform.core.LockedCompletedBooking;
import unichope.platform.core.LifecycleFailure;

@Tag("postgres")
class PostgresPrivateNoteLockTest extends LifecyclePgFixture {
    @Test
    void requiresActiveCallerTransactionBeforeQuerying() {
        var target=completedBooking();
        assertThrows(IllegalTransactionStateException.class,()->lifecycle.lockForPrivateNote(T1,target));
        assertEquals(INVALID_INPUT,assertThrows(LifecycleFailure.class,()->lifecycle.lockForPrivateNote(null,target)).code());
        assertEquals(INVALID_INPUT,assertThrows(LifecycleFailure.class,()->lifecycle.lockForPrivateNote(T1,null)).code());
    }

    @Test
    void rejectsCallerTransactionBoundToAnotherDataSource() {
        var target=completedBooking();
        DriverManagerDataSource other=new DriverManagerDataSource(
                System.getProperty("core.test.jdbcUrl"),System.getProperty("core.test.user", ""),System.getProperty("core.test.password", ""));
        Properties properties=new Properties();
        properties.setProperty("currentSchema",schema);
        other.setConnectionProperties(properties);
        assertThrows(IllegalTransactionStateException.class,()->new TransactionTemplate(new DataSourceTransactionManager(other))
                .execute(status->lifecycle.lockForPrivateNote(T1,target)));
    }

    @Test
    void joinsCallerTransactionAndRetainsLocksUntilOuterRollback() throws Exception {
        var target=completedBooking();
        TransactionTemplate caller=transaction();
        CountDownLatch returnedFromSeam=new CountDownLatch(1), finishCaller=new CountDownLatch(1);
        Future<LockedCompletedBooking> outer=workers.submit(()->caller.execute(status->{
            var context=lifecycle.lockForPrivateNote(T1,target);
            db.sql("UPDATE users SET name='Temporary' WHERE id=:id").param("id",T1).update();
            returnedFromSeam.countDown();
            try { assertTrue(finishCaller.await(10,TimeUnit.SECONDS)); }
            catch(InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
            status.setRollbackOnly();
            return context;
        }));
        try {
            assertTrue(returnedFromSeam.await(5,TimeUnit.SECONDS));
            Future<Integer> contender=workers.submit(()->db.sql("UPDATE bookings SET created_at=created_at WHERE id=:id").param("id",target).update());
            awaitBlocked(1);
            finishCaller.countDown();
            var context=outer.get(10,TimeUnit.SECONDS);
            assertEquals(target,context.bookingId());
            assertEquals(1,contender.get(10,TimeUnit.SECONDS));
            assertEquals("Synthetic",db.sql("SELECT name FROM users WHERE id=:id").param("id",T1).query(String.class).single());
        } finally { finishCaller.countDown(); }
    }

    @Test
    void returnsOnlyTheLockedCompletedBookingIdentifiers() {
        var target=completedBooking();
        var context=transaction().execute(status->lifecycle.lockForPrivateNote(T1,target));
        assertEquals(target,context.bookingId());
        assertEquals(T1,context.tutorId());
        assertEquals(S1,context.studentId());
        assertEquals(M,context.moduleId());
        assertEquals(db.sql("SELECT slot_id FROM bookings WHERE id=:id").param("id",target).query(java.util.UUID.class).single(),context.slotId());
    }

    @Test
    void rejectsWrongRoleInactiveTutorAndForeignOrMissingBooking() {
        var target=completedBooking();
        assertFailureInCallerTransaction(ACCESS_DENIED,S1,target);
        assertFailureInCallerTransaction(ACCESS_DENIED,S1,id(998));
        db.sql("UPDATE users SET active=false WHERE id=:id").param("id",T1).update();
        assertFailureInCallerTransaction(ACCESS_DENIED,T1,target);
        db.sql("UPDATE users SET active=true WHERE id=:id").param("id",T1).update();
        assertFailureInCallerTransaction(NOT_FOUND,T2,target);
        assertFailureInCallerTransaction(NOT_FOUND,T1,id(999));
    }

    @Test
    void rejectsOwnedBookingUnlessBothBookingAndSlotAreCompleted() {
        var target=completedBooking();
        db.sql("UPDATE bookings SET status='ACTIVE' WHERE id=:id").param("id",target).update();
        assertFailureInCallerTransaction(INVALID_STATE,T1,target);
        db.sql("UPDATE bookings SET status='COMPLETED' WHERE id=:id").param("id",target).update();
        db.sql("UPDATE slots SET status='BOOKED' WHERE id=(SELECT slot_id FROM bookings WHERE id=:id)").param("id",target).update();
        assertFailureInCallerTransaction(INVALID_STATE,T1,target);
    }

    @Test
    void rejectsNonReadCommittedCallerBeforeTakingRowLocks() {
        var target=completedBooking();
        TransactionTemplate serializable=transaction();
        serializable.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        assertThrows(IllegalStateException.class,()->serializable.execute(status->lifecycle.lockForPrivateNote(T1,target)));
    }

    @Test
    void observesTutorDeactivationAfterWaitingForTheUserLock() throws Exception {
        var target=completedBooking();
        try(Connection held=holder(T1)) {
            Future<Object> pending=launch(()->transaction().execute(status->lifecycle.lockForPrivateNote(T1,target)));
            awaitBlocked(1);
            on(held).sql("UPDATE users SET active=false WHERE id=:id").param("id",T1).update();
            held.commit();
            assertEquals(ACCESS_DENIED,assertInstanceOf(LifecycleFailure.class,pending.get(10,TimeUnit.SECONDS)).code());
        }
    }

    @Test
    void observesBookingStateAndOwnershipChangesAfterWaitingForTheirRows() throws Exception {
        var target=completedBooking();
        var slotId=db.sql("SELECT slot_id FROM bookings WHERE id=:id").param("id",target).query(java.util.UUID.class).single();
        try(Connection held=source.getConnection()) {
            held.setAutoCommit(false);
            on(held).sql("SELECT id FROM bookings WHERE id=:id FOR UPDATE").param("id",target).query(java.util.UUID.class).single();
            Future<Object> pending=launch(()->transaction().execute(status->lifecycle.lockForPrivateNote(T1,target)));
            awaitBlocked(1);
            on(held).sql("UPDATE bookings SET status='ACTIVE' WHERE id=:id").param("id",target).update();
            held.commit();
            assertEquals(INVALID_STATE,assertInstanceOf(LifecycleFailure.class,pending.get(10,TimeUnit.SECONDS)).code());
        }
        db.sql("UPDATE bookings SET status='COMPLETED' WHERE id=:id").param("id",target).update();
        try(Connection held=source.getConnection()) {
            held.setAutoCommit(false);
            on(held).sql("SELECT id FROM slots WHERE id=:id FOR UPDATE").param("id",slotId).query(java.util.UUID.class).single();
            Future<Object> pending=launch(()->transaction().execute(status->lifecycle.lockForPrivateNote(T1,target)));
            awaitBlocked(1);
            on(held).sql("UPDATE slots SET tutor_id=:tutor WHERE id=:id").param("tutor",T2).param("id",slotId).update();
            held.commit();
            assertEquals(NOT_FOUND,assertInstanceOf(LifecycleFailure.class,pending.get(10,TimeUnit.SECONDS)).code());
        }
    }

    @Test
    void acquiresUserModuleSlotAndBookingLocksInProtocolOrder() throws Exception {
        var target=completedBooking();
        var slotId=db.sql("SELECT slot_id FROM bookings WHERE id=:id").param("id",target).query(java.util.UUID.class).single();
        assertSortedUsersBeforeSecondUser(target);
        assertBlockedAt(target,"SELECT id FROM modules WHERE id=:id FOR UPDATE",M,
                "UPDATE users SET name=name WHERE id=:id",T1);
        assertBlockedAt(target,"SELECT id FROM slots WHERE id=:id FOR UPDATE",slotId,
                "UPDATE modules SET name=name WHERE id=:id",M);
        assertBlockedAt(target,"SELECT id FROM bookings WHERE id=:id FOR UPDATE",target,
                "UPDATE slots SET status=status WHERE id=:id",slotId);
    }

    @Test
    void callerWriteFailureAfterTheLockRollsBackTheWholeTransaction() {
        var target=completedBooking();
        String before=db.sql("SELECT name FROM users WHERE id=:id").param("id",T1).query(String.class).single();
        db.sql("CREATE OR REPLACE FUNCTION reject_caller_write() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'synthetic caller failure' USING ERRCODE='P0001'; END $$").update();
        db.sql("CREATE TRIGGER reject_caller_write BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION reject_caller_write()").update();
        try {
            DataAccessException error=assertThrows(DataAccessException.class,()->transaction().execute(status->{
                lifecycle.lockForPrivateNote(T1,target);
                db.sql("UPDATE users SET name='Temporary' WHERE id=:id").param("id",T1).update();
                return null;
            }));
            assertTrue(error.getMessage().contains("synthetic caller failure"));
            assertEquals(before,db.sql("SELECT name FROM users WHERE id=:id").param("id",T1).query(String.class).single());
        } finally {
            db.sql("DROP TRIGGER reject_caller_write ON users").update();
            db.sql("DROP FUNCTION reject_caller_write()").update();
        }
    }

    @Test
    void mapsLockTimeoutOnlyAfterCallerTransactionRollsBack() throws Exception {
        var target=completedBooking();
        CountDownLatch caughtAfterBoundary=new CountDownLatch(1);
        try(Connection held=holder(T1)) {
            long start=System.nanoTime();
            Future<Object> pending=workers.submit(()->{
                try {
                    transaction().execute(status->lifecycle.lockForPrivateNote(T1,target));
                    return null;
                } catch(LifecycleFailure error) {
                    boolean transactionActive=TransactionSynchronizationManager.isActualTransactionActive();
                    caughtAfterBoundary.countDown();
                    return new Object[]{error,transactionActive};
                }
            });
            awaitBlocked(1);
            var outcome=assertInstanceOf(Object[].class,pending.get(10,TimeUnit.SECONDS));
            long elapsed=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);
            var error=assertInstanceOf(LifecycleFailure.class,outcome[0]);
            assertEquals(TEMPORARILY_UNAVAILABLE,error.code());
            assertFalse((boolean)outcome[1],"Failure must escape the caller transaction before response mapping");
            assertTrue(caughtAfterBoundary.await(1,TimeUnit.SECONDS));
            assertTrue(elapsed>=4000&&elapsed<10000,"Expected bounded five-second lock timeout: "+elapsed);
            held.commit();
        }
    }

    @Test
    void waitsOnUsersBeforeModuleAndRetainsEarlierLocks() throws Exception {
        var target=completedBooking();
        try(Connection moduleHolder=source.getConnection()) {
            moduleHolder.setAutoCommit(false);
            on(moduleHolder).sql("SELECT id FROM modules WHERE id=:id FOR UPDATE").param("id",M).query(java.util.UUID.class).single();
            Future<Object> pending=launch(()->transaction().execute(status->lifecycle.lockForPrivateNote(T1,target)));
            awaitBlocked(1);
            try(Connection probe=source.getConnection()) {
                probe.setAutoCommit(false);
                JdbcClient probeJdbc=on(probe);
                probeJdbc.sql("SET LOCAL lock_timeout='200ms'").update();
                assertThrows(DataAccessException.class,()->probeJdbc.sql("UPDATE users SET name=name WHERE id=:id").param("id",T1).update());
            }
            moduleHolder.commit();
            assertNotNull(pending.get(10,TimeUnit.SECONDS));
        }
    }

    private java.util.UUID completedBooking() {
        var slot=slot(T1,START,END,"COMPLETED");
        return booking(S1,slot,"COMPLETED");
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(new DataSourceTransactionManager(source));
    }

    private void assertFailureInCallerTransaction(LifecycleFailure.Code expected,java.util.UUID tutor,java.util.UUID booking) {
        LifecycleFailure error=assertThrows(LifecycleFailure.class,()->transaction().execute(status->lifecycle.lockForPrivateNote(tutor,booking)));
        assertEquals(expected,error.code());
        assertNull(error.getCause());
    }

    private void assertSortedUsersBeforeSecondUser(java.util.UUID target) throws Exception {
        try(Connection held=holder(S1)) {
            Future<Object> pending=launch(()->transaction().execute(status->lifecycle.lockForPrivateNote(T1,target)));
            awaitBlocked(1);
            assertProbeTimesOut("UPDATE users SET name=name WHERE id=:id",T1);
            held.commit();
            assertNotNull(pending.get(10,TimeUnit.SECONDS));
        }
    }

    private void assertBlockedAt(java.util.UUID target,String heldSql,java.util.UUID heldId,String probeSql,java.util.UUID probeId) throws Exception {
        try(Connection held=source.getConnection()) {
            held.setAutoCommit(false);
            on(held).sql(heldSql).param("id",heldId).query(java.util.UUID.class).single();
            Future<Object> pending=launch(()->transaction().execute(status->lifecycle.lockForPrivateNote(T1,target)));
            awaitBlocked(1);
            assertProbeTimesOut(probeSql,probeId);
            held.commit();
            assertNotNull(pending.get(10,TimeUnit.SECONDS));
        }
    }

    private void assertProbeTimesOut(String sql,java.util.UUID id) throws Exception {
        try(Connection probe=source.getConnection()) {
            probe.setAutoCommit(false);
            JdbcClient probeJdbc=on(probe);
            probeJdbc.sql("SET LOCAL lock_timeout='200ms'").update();
            assertThrows(DataAccessException.class,()->probeJdbc.sql(sql).param("id",id).update());
        }
    }
}
