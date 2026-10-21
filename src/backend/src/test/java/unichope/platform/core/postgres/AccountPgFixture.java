package unichope.platform.core.postgres;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

abstract class AccountPgFixture {
    static final UUID T1=id(1), T2=id(2), S1=id(3), S2=id(4), ADMIN=id(5), M=id(10), M2=id(11);
    static final Instant N=Instant.parse("2026-10-21T00:00:00Z");
    static final Instant START=N.plusSeconds(3600), END=N.plusSeconds(7200);
    DriverManagerDataSource source;
    JdbcClient db;
    String schema;
    final MovingClock clock=new MovingClock();
    PostgresConsultationLifecycle lifecycle;
    PostgresAccountStorage accounts;
    final ExecutorService workers=Executors.newFixedThreadPool(2);

    @BeforeEach
    void prepare() throws Exception {
        String url=System.getProperty("core.test.jdbcUrl");
        assertNotNull(url, "Explicit disposable core.test.jdbcUrl is required");
        source=new DriverManagerDataSource(url, System.getProperty("core.test.user", ""), System.getProperty("core.test.password", ""));
        schema="account_test_"+UUID.randomUUID().toString().replace("-", "");
        try(Connection c=source.getConnection(); var statement=c.createStatement()) { statement.execute("CREATE SCHEMA "+schema); }
        Properties properties=new Properties();
        properties.setProperty("currentSchema", schema);
        properties.setProperty("ApplicationName", schema);
        source.setConnectionProperties(properties);
        Flyway.configure().dataSource(source).defaultSchema(schema).schemas(schema).locations("classpath:db/core-review", "classpath:db/auth-review").load().migrate();
        db=JdbcClient.create(source);
        for(UUID actor:List.of(T1,T2,S1,S2,ADMIN)) {
            String role=actor.equals(ADMIN)?"ADMIN":actor.equals(T1)||actor.equals(T2)?"TUTOR":"STUDENT";
            db.sql("INSERT INTO users (id,name,email,role,active,password_hash) VALUES (:id,'Synthetic',:email,:role,true,'synthetic-H1')").param("id",actor).param("email",actor+"@example.edu").param("role",role).update();
        }
        for(UUID module:List.of(M,M2)) db.sql("INSERT INTO modules VALUES (:id,:code,'Synthetic module',true)").param("id",module).param("code","CS"+module.getLeastSignificantBits()).update();
        for(UUID tutor:List.of(T1,T2)) for(UUID module:List.of(M,M2)) db.sql("INSERT INTO tutor_modules VALUES (:t,:m)").param("t",tutor).param("m",module).update();
        lifecycle=new PostgresConsultationLifecycle(source,clock);
        accounts=new PostgresAccountStorage(source,clock);
    }

    @AfterEach
    void cleanup() throws Exception {
        workers.shutdownNow();
        assertTrue(workers.awaitTermination(15,TimeUnit.SECONDS),"Workers did not terminate");
        if(schema!=null) try(Connection c=source.getConnection(); var statement=c.createStatement()) { statement.execute("DROP SCHEMA "+schema+" CASCADE"); }
    }

    UUID slot(UUID owner, Instant start, Instant end, String status) { return slot(owner,M,start,end,status); }
    UUID slot(UUID owner, UUID module, Instant start, Instant end, String status) {
        UUID id=UUID.randomUUID();
        db.sql("INSERT INTO slots VALUES (:id,:t,:m,:s,:e,:status)").param("id",id).param("t",owner).param("m",module)
                .param("s",start.atOffset(ZoneOffset.UTC)).param("e",end.atOffset(ZoneOffset.UTC)).param("status",status).update();
        return id;
    }
    UUID booking(UUID student, UUID slot, String status) {
        UUID id=UUID.randomUUID();
        db.sql("INSERT INTO bookings VALUES (:id,:s,:slot,:at,:status)").param("id",id).param("s",student).param("slot",slot)
                .param("at",N.atOffset(ZoneOffset.UTC)).param("status",status).update();
        return id;
    }
    void clearConsultations() { db.sql("DELETE FROM bookings").update(); db.sql("DELETE FROM slots").update(); clock.now.set(N); }
    long count(String table) { return db.sql("SELECT count(*) FROM "+table).query(Long.class).single(); }
    Connection holder(UUID... ids) throws Exception {
        Connection connection=source.getConnection(); connection.setAutoCommit(false);
        try {
            JdbcClient jdbc=on(connection);
            for(UUID id:java.util.Arrays.stream(ids).distinct().sorted().toList()) jdbc.sql("SELECT id FROM users WHERE id=:id FOR UPDATE").param("id",id).query(UUID.class).single();
            return connection;
        } catch(Exception error) { connection.close(); throw error; }
    }
    JdbcClient on(Connection connection) { return JdbcClient.create(new SingleConnectionDataSource(connection,true)); }
    Future<Object> launch(Callable<?> work) {
        return workers.submit(() -> { try { return work.call(); } catch(unichope.platform.core.AccountFailure failure) { return failure; } });
    }
    void awaitBlocked(int count) {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
        while(System.nanoTime()<deadline) {
            long waiting=db.sql("SELECT count(*) FROM pg_stat_activity WHERE application_name=:app AND cardinality(pg_blocking_pids(pid))>0")
                    .param("app",schema).query(Long.class).single();
            if(waiting>=count) return;
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
        }
        fail("Did not observe "+count+" independently blocked PostgreSQL callers");
    }
    List<Object> race(UUID held, Callable<?> first, Callable<?> second) throws Exception {
        try(Connection holder=holder(held)) {
            Future<Object> a=launch(first); awaitBlocked(1);
            Future<Object> b=launch(second); awaitBlocked(2);
            holder.commit();
            return List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
        }
    }
    static UUID id(int number) { return new UUID(0,number); }
    static final class MovingClock extends Clock {
        final AtomicReference<Instant> now=new AtomicReference<>(N);
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }
}
