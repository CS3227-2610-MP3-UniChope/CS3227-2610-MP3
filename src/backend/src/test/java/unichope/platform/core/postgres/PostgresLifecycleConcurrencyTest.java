package unichope.platform.core.postgres;

import static org.junit.jupiter.api.Assertions.*;
import static unichope.platform.core.LifecycleFailure.Code.*;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import unichope.platform.core.LifecycleFailure;

@Tag("postgres")
class PostgresLifecycleConcurrencyTest extends LifecyclePgFixture {
    @Test
    void td17EmptyScheduleConcurrentOverlapsSerializeOnTutorParent() throws Exception {
        exactlyOne(race(T1,()->lifecycle.createSlot(T1,M,START,END),
                ()->lifecycle.createSlot(T1,M,START.plusSeconds(1800),END.plusSeconds(1800))),SLOT_OVERLAP);
        assertEquals(1,count("slots")); assertEquals(0,count("bookings"));
    }

    @Test
    void td18SameSlotConcurrentBookingsHaveOneWinnerAndConsistentPair() throws Exception {
        UUID target=slot(T1,START,END,"AVAILABLE");
        exactlyOne(race(T1,()->lifecycle.bookAvailableSlot(S1,target),()->lifecycle.bookAvailableSlot(S2,target)),INVALID_STATE);
        assertEquals(1,count("bookings")); assertEquals("BOOKED",slotStatus(target));
        assertEquals(1L,db.sql("SELECT count(*) FROM bookings WHERE slot_id=:id AND status='ACTIVE'").param("id",target).query(Long.class).single());
    }

    @Test
    void td19SameStudentDifferentTutorsSerializeCrossSlotOverlap() throws Exception {
        UUID a=slot(T1,START,END,"AVAILABLE"), b=slot(T2,START.plusSeconds(1800),END.plusSeconds(1800),"AVAILABLE");
        exactlyOne(race(S1,()->lifecycle.bookAvailableSlot(S1,a),()->lifecycle.bookAvailableSlot(S1,b)),BOOKING_OVERLAP);
        assertEquals(1,count("bookings"));
        assertEquals(1L,db.sql("SELECT count(*) FROM slots WHERE status='BOOKED'").query(Long.class).single());
        assertEquals(1L,db.sql("SELECT count(*) FROM slots WHERE status='AVAILABLE'").query(Long.class).single());
    }

    @Test
    void td20BookingVersusSlotCancellationBothQueueOrdersRemainConsistent() throws Exception {
        for(boolean bookFirst:List.of(true,false)) {
            clearConsultations(); UUID target=slot(T1,START,END,"AVAILABLE");
            Callable<?> book=()->lifecycle.bookAvailableSlot(S1,target), cancel=()->lifecycle.cancelAvailableSlot(T1,target);
            exactlyOne(race(T1,bookFirst?book:cancel,bookFirst?cancel:book),INVALID_STATE);
            assertEquals(bookFirst?"BOOKED":"CANCELLED",slotStatus(target));
            assertEquals(bookFirst?1:0,count("bookings"));
        }
    }

    @Test
    void td21CancellationVersusCompletionBothQueueOrdersRemainConsistent() throws Exception {
        for(boolean cancelFirst:List.of(true,false)) {
            clearConsultations(); UUID target=slot(T1,START,END,"BOOKED"), booking=booking(S1,target,"ACTIVE");
            Callable<?> cancel=()->lifecycle.cancelActiveBooking(S1,booking), complete=()->lifecycle.completeActiveBooking(T1,booking);
            exactlyOne(race(T1,cancelFirst?cancel:complete,cancelFirst?complete:cancel),INVALID_STATE);
            assertEquals(cancelFirst?"CANCELLED":"COMPLETED",bookingStatus(booking));
            assertEquals(cancelFirst?"AVAILABLE":"COMPLETED",slotStatus(target)); assertEquals(1,count("bookings"));
        }
    }

    @Test
    void td22EveryOperationRechecksActorAfterWaitingForOrderedUserLocks() throws Exception {
        for(int operation=0;operation<5;operation++) {
            clearConsultations(); db.sql("UPDATE users SET active=true").update();
            int op=operation; UUID target=slot(T1,START,END,op==1||op==2?"AVAILABLE":"BOOKED");
            UUID booking=op==3||op==4?booking(S1,target,"ACTIVE"):id(999);
            UUID actor=op==2||op==3?S1:T1;
            String before=snapshot();
            try(Connection held=holder(T1,S1)) {
                Future<Object> caller=launch(()->invoke(op,target,booking)); awaitBlocked(1);
                on(held).sql("UPDATE users SET active=false WHERE id=:id").param("id",actor).update(); held.commit();
                assertEquals(ACCESS_DENIED,assertInstanceOf(LifecycleFailure.class,caller.get(15,TimeUnit.SECONDS)).code());
            }
            assertEquals(before,snapshot());
        }
    }

    @Test
    void td23CreateAndBookRecheckAssignmentModuleAndTutorEligibilityAfterWait() throws Exception {
        for(boolean create:List.of(true,false)) for(String change:List.of("assignment","module","tutor")) {
            if(create&&change.equals("tutor")) continue; // actor deactivation is TD-22, not MODULE_UNAVAILABLE.
            clearConsultations(); db.sql("UPDATE users SET active=true").update(); db.sql("UPDATE modules SET active=true").update();
            db.sql("INSERT INTO tutor_modules VALUES (:t,:m) ON CONFLICT DO NOTHING").param("t",T1).param("m",M).update();
            UUID target=slot(T1,START,END,"AVAILABLE"); String before=snapshot();
            try(Connection held=holder(T1,S1)) {
                on(held).sql("SELECT id FROM modules WHERE id=:id FOR UPDATE").param("id",M).query(UUID.class).single();
                Future<Object> caller=launch(()->create?lifecycle.createSlot(T1,M,END,END.plusSeconds(1800)):lifecycle.bookAvailableSlot(S1,target));
                awaitBlocked(1);
                switch(change) {
                    case "assignment" -> on(held).sql("DELETE FROM tutor_modules WHERE tutor_id=:t AND module_id=:m").param("t",T1).param("m",M).update();
                    case "module" -> on(held).sql("UPDATE modules SET active=false WHERE id=:id").param("id",M).update();
                    case "tutor" -> on(held).sql("UPDATE users SET active=false WHERE id=:id").param("id",T1).update();
                    default -> throw new IllegalArgumentException();
                }
                held.commit();
                assertEquals(MODULE_UNAVAILABLE,assertInstanceOf(LifecycleFailure.class,caller.get(15,TimeUnit.SECONDS)).code());
            }
            assertEquals(before,snapshot());
        }
    }

    @Test
    void td24ClockIsReadAfterLocksForCreateBookAndBookingCancellation() throws Exception {
        for(int operation:List.of(0,2,3)) {
            clearConsultations(); UUID target=slot(T1,START,END,operation==3?"BOOKED":"AVAILABLE");
            UUID b=operation==3?booking(S1,target,"ACTIVE"):id(999);
            String before=snapshot();
            try(Connection held=holder(T1,S1)) {
                Future<Object> caller=launch(()->invoke(operation,target,b)); awaitBlocked(1);
                clock.now.set(START); held.commit();
                assertEquals(operation==0?INVALID_TIME:INVALID_STATE,assertInstanceOf(LifecycleFailure.class,caller.get(15,TimeUnit.SECONDS)).code());
            }
            assertEquals(before,snapshot());
        }
    }

    @Test
    void td25SecondWriteFailureRollsBackBookingInsertCancelAndCompletion() {
        for(int operation:List.of(2,3,4)) {
            clearConsultations(); UUID target=slot(T1,START,END,operation==2?"AVAILABLE":"BOOKED");
            UUID booking=operation==2?id(999):booking(S1,target,"ACTIVE");
            String expected=operation==2?"ACTIVE":operation==3?"CANCELLED":"COMPLETED";
            db.sql("""
                    CREATE OR REPLACE FUNCTION reject_second_write() RETURNS trigger LANGUAGE plpgsql AS $$
                    BEGIN
                        IF NOT EXISTS (SELECT 1 FROM bookings WHERE slot_id=NEW.id AND status='%s') THEN
                            RAISE EXCEPTION 'first write was not observed' USING ERRCODE='P0002';
                        END IF;
                        RAISE EXCEPTION 'synthetic second write failure' USING ERRCODE='P0001';
                    END $$
                    """.formatted(expected)).update();
            db.sql("CREATE TRIGGER reject_second_write BEFORE UPDATE ON slots FOR EACH ROW EXECUTE FUNCTION reject_second_write()").update();
            String before=snapshot();
            try {
                DataAccessException error=assertThrows(DataAccessException.class,()->invoke(operation,target,booking));
                assertTrue(error.getMessage().contains("synthetic second write failure"),"Trigger must observe first write before failing second");
                assertEquals(before,snapshot());
            } finally { db.sql("DROP TRIGGER reject_second_write ON slots").update(); }
        }
    }

    @Test
    void td26RealLockTimeoutRollsBackAndSamePhysicalConnectionCanRecover() throws Exception {
        try(Connection physical=source.getConnection(); Connection held=holder(T1)) {
            var sameConnection=new SingleConnectionDataSource(physical,true);
            var blocked=new PostgresConsultationLifecycle(sameConnection,clock);
            String before=snapshot(); long started=System.nanoTime();
            Future<Object> caller=launch(()->blocked.createSlot(T1,M,START,END)); awaitBlocked(1);
            var error=assertInstanceOf(LifecycleFailure.class,caller.get(12,TimeUnit.SECONDS));
            assertEquals(TEMPORARILY_UNAVAILABLE,error.code());
            long elapsed=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started);
            assertTrue(elapsed>=4000&&elapsed<12000,"Bounded actual 5-second lock timeout: "+elapsed);
            assertEquals(before,snapshot()); held.commit();
            assertTrue(physical.getAutoCommit());
            assertEquals("0",on(physical).sql("SHOW lock_timeout").query(String.class).single());
            assertEquals("0",on(physical).sql("SHOW statement_timeout").query(String.class).single());
            assertNotNull(blocked.createSlot(T1,M,START,END)); assertEquals(1,count("slots"));
            assertEquals("0",on(physical).sql("SHOW lock_timeout").query(String.class).single());
        }
    }

    @Test
    void td27MappedSqlstatesRollbackAndLocalTimeoutsResetAfterCommitOrFailure() throws Exception {
        for(String state:List.of("55P03","40P01","40001","57014")) {
            clearConsultations(); UUID target=slot(T1,START,END,"AVAILABLE");
            db.sql("""
                    CREATE OR REPLACE FUNCTION mapped_error() RETURNS trigger LANGUAGE plpgsql AS $$
                    BEGIN
                        IF current_setting('lock_timeout') <> '5s' OR current_setting('statement_timeout') <> '10s' THEN
                            RAISE EXCEPTION 'missing local transaction timeout configuration';
                        END IF;
                        RAISE EXCEPTION 'synthetic-private-detail' USING ERRCODE='%s';
                    END $$
                    """.formatted(state)).update();
            db.sql("CREATE TRIGGER mapped_error BEFORE UPDATE ON slots FOR EACH ROW EXECUTE FUNCTION mapped_error()").update();
            try(Connection physical=source.getConnection()) {
                var local=new PostgresConsultationLifecycle(new SingleConnectionDataSource(physical,true),clock);
                LifecycleFailure error=failure(TEMPORARILY_UNAVAILABLE,()->local.bookAvailableSlot(S1,target));
                assertFalse(error.getMessage().contains("synthetic-private-detail")); assertFalse(error.getMessage().contains(target.toString()));
                assertNull(error.getCause(),"Public business failure should not retain SQL diagnostics");
                assertEquals("0",on(physical).sql("SHOW lock_timeout").query(String.class).single());
                assertEquals("0",on(physical).sql("SHOW statement_timeout").query(String.class).single());
            } finally { db.sql("DROP TRIGGER mapped_error ON slots").update(); }
        }
        clearConsultations();
        try(Connection physical=source.getConnection()) {
            var local=new PostgresConsultationLifecycle(new SingleConnectionDataSource(physical,true),clock);
            local.createSlot(T1,M,START,END);
            assertEquals("0",on(physical).sql("SHOW lock_timeout").query(String.class).single());
            assertEquals("0",on(physical).sql("SHOW statement_timeout").query(String.class).single());
        }
    }

    private Object invoke(int operation,UUID slot,UUID booking) {
        return switch(operation) {
            case 0 -> lifecycle.createSlot(T1,M,START,END);
            case 1 -> lifecycle.cancelAvailableSlot(T1,slot);
            case 2 -> lifecycle.bookAvailableSlot(S1,slot);
            case 3 -> lifecycle.cancelActiveBooking(S1,booking);
            case 4 -> lifecycle.completeActiveBooking(T1,booking);
            default -> throw new IllegalArgumentException();
        };
    }
}
