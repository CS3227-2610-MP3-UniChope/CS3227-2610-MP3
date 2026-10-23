package unichope.platform.core.postgres;

import static unichope.platform.core.AccountFailure.Code.*;
import java.sql.SQLException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import unichope.platform.core.*;
import unichope.platform.core.model.Validation;
import unichope.platform.core.model.user.Role;
import unichope.platform.core.model.user.User;

/** PostgreSQL account boundary, coordinated with consultation operations by user locks. */
public final class PostgresAccountStorage implements AccountStorage {
    private static final Set<String> CONTENTION = Set.of("55P03", "40P01", "40001", "57014");
    private static final String SELECT = "SELECT id,name,email,role,active,password_hash,session_generation,must_change_password FROM users";
    private final JdbcClient jdbc;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public PostgresAccountStorage(DataSource source, Clock clock) {
        jdbc = JdbcClient.create(source);
        this.clock = clock;
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override public Optional<AccountCredentialSnapshot> findCredentialByEmail(String normalizedEmail) {
        String email = canonical(normalizedEmail);
        return safe(() -> jdbc.sql(SELECT + " WHERE email=:email").param("email", email)
                .query((row, index) -> new AccountCredentialSnapshot(PostgresUserRepository.map(row,index), row.getString("password_hash"), row.getLong("session_generation"), row.getBoolean("must_change_password"))).optional());
    }

    @Override public Optional<AccountState> findAccountState(UUID userId) {
        required(userId);
        return safe(() -> state(userId));
    }

    @Override public User registerStudent(String name, String email, String encodedHash) {
        String storedName = name(name), storedEmail = canonical(email);
        hash(encodedHash);
        return mutate(() -> insert(Role.STUDENT, storedName, storedEmail, encodedHash));
    }

    @Override public User provisionAccount(AdminContext admin, Role role, String name, String email, String encodedHash) {
        context(admin); required(role);
        String storedName = name(name), storedEmail = canonical(email);
        hash(encodedHash);
        return mutate(() -> {
            lockUsers(admin.actorId());
            requireAdmin(admin);
            if (role == Role.ADMIN) throw fail(ACCESS_DENIED);
            return insert(role, storedName, storedEmail, encodedHash);
        });
    }

    @Override public AccountState setAccountActive(AdminContext admin, UUID targetId, boolean active) {
        context(admin); required(targetId);
        return mutate(() -> {
            lockUsers(admin.actorId(), targetId);
            requireAdmin(admin);
            AccountState current = state(targetId).orElseThrow(() -> fail(NOT_FOUND));
            if (current.user().role() == Role.ADMIN) throw fail(ACCESS_DENIED);
            if (current.user().isActive() == active) return current;
            long generation = current.sessionGeneration();
            if (!active) {
                if (blocked(current.user()) || generation == Long.MAX_VALUE) throw fail(INVALID_STATE);
                generation++;
            }
            jdbc.sql("UPDATE users SET active=:active,session_generation=:generation WHERE id=:id")
                    .param("active", active).param("generation", generation).param("id", targetId).update();
            return state(targetId).orElseThrow(() -> fail(NOT_FOUND));
        });
    }

    @Override public AccountState confirmVerifiedLogin(UUID userId, Role observedRole, String observedHash, long observedGeneration) {
        required(userId, observedRole); hash(observedHash);
        if (observedGeneration < 0) throw fail(INVALID_INPUT);
        return mutate(() -> {
            lockUsers(userId);
            AccountCredentialSnapshot current = jdbc.sql(SELECT + " WHERE id=:id").param("id", userId)
                    .query((row,index) -> new AccountCredentialSnapshot(PostgresUserRepository.map(row,index), row.getString("password_hash"), row.getLong("session_generation"), row.getBoolean("must_change_password")))
                    .optional().orElseThrow(() -> fail(LOGIN_REJECTED));
            if (!current.user().isActive() || current.user().role() != observedRole
                    || current.sessionGeneration() != observedGeneration || !current.encodedHash().equals(observedHash)) throw fail(LOGIN_REJECTED);
            return new AccountState(current.user(),current.sessionGeneration(),current.mustChangePassword());
        });
    }

    @Override public AccountState changeTutorPassword(UUID userId, long expectedGeneration, String encodedHash) {
        required(userId); hash(encodedHash);
        if (expectedGeneration < 0) throw fail(INVALID_INPUT);
        return mutate(() -> {
            lockUsers(userId);
            AccountState current = state(userId).orElseThrow(() -> fail(ACCESS_DENIED));
            if (current.user().role() != Role.TUTOR || !current.user().isActive()
                    || current.sessionGeneration() != expectedGeneration || !current.mustChangePassword()
                    || expectedGeneration == Long.MAX_VALUE) throw fail(ACCESS_DENIED);
            jdbc.sql("UPDATE users SET password_hash=:hash,must_change_password=false,session_generation=:generation WHERE id=:id")
                    .param("hash", encodedHash).param("generation", expectedGeneration + 1).param("id", userId).update();
            return state(userId).orElseThrow(() -> fail(STORAGE_FAILURE));
        });
    }

    private User insert(Role role, String name, String email, String hash) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO users (id,name,email,role,active,password_hash,session_generation,must_change_password) VALUES (:id,:name,:email,:role,true,:hash,0,:mustChange)")
                .param("id",id).param("name",name).param("email",email).param("role",role.name()).param("hash",hash).param("mustChange",role == Role.TUTOR).update();
        return state(id).orElseThrow(() -> fail(STORAGE_FAILURE)).user();
    }
    private Optional<AccountState> state(UUID id) {
        return jdbc.sql("SELECT id,name,email,role,active,session_generation,must_change_password FROM users WHERE id=:id").param("id",id)
                .query((row,index) -> new AccountState(PostgresUserRepository.map(row,index),row.getLong("session_generation"),row.getBoolean("must_change_password"))).optional();
    }
    private void requireAdmin(AdminContext admin) {
        AccountState actor = state(admin.actorId()).orElseThrow(() -> fail(ACCESS_DENIED));
        if (!actor.user().isActive() || actor.user().role() != Role.ADMIN || actor.sessionGeneration() != admin.expectedGeneration()) throw fail(ACCESS_DENIED);
    }
    private void lockUsers(UUID... ids) {
        jdbc.sql("SELECT id FROM users WHERE id IN (:ids) ORDER BY id FOR UPDATE")
                .param("ids",Arrays.stream(ids).distinct().toList()).query(UUID.class).list();
    }
    private boolean blocked(User user) {
        if (jdbc.sql("SELECT EXISTS (SELECT 1 FROM bookings b JOIN slots s ON s.id=b.slot_id WHERE b.status='ACTIVE' AND "
                + (user.role() == Role.STUDENT ? "b.student_id=:id)" : "s.tutor_id=:id)"))
                .param("id",user.id()).query(Boolean.class).single()) return true;
        // Stored starts lie on PostgreSQL's microsecond grid. Floor the fresh clock
        // so JDBC rounding cannot erase a strictly future submicrosecond interval.
        return user.role() == Role.TUTOR && jdbc.sql("SELECT EXISTS (SELECT 1 FROM slots WHERE tutor_id=:id AND status='AVAILABLE' AND start_time>:now)")
                .param("id",user.id()).param("now",clock.instant().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC)).query(Boolean.class).single();
    }
    private <T> T mutate(Supplier<T> work) {
        return safe(() -> transaction.execute(status -> {
            jdbc.sql("SET LOCAL lock_timeout='5s'").update();
            jdbc.sql("SET LOCAL statement_timeout='10s'").update();
            return work.get();
        }));
    }
    private <T> T safe(Supplier<T> work) {
        try { return work.get(); }
        catch (DataAccessException | TransactionException error) {
            for (Throwable cause=error; cause!=null; cause=cause.getCause()) {
                if (cause instanceof SQLException sql && CONTENTION.contains(sql.getSQLState())) throw fail(TEMPORARILY_UNAVAILABLE);
                if (cause instanceof PSQLException sql && "23505".equals(sql.getSQLState()) && sql.getServerErrorMessage()!=null
                        && "users_email_unique".equals(sql.getServerErrorMessage().getConstraint())) throw fail(ACCOUNT_UNAVAILABLE);
            }
            // PostgreSQL DETAIL may contain the complete failing row, including the hash.
            throw fail(STORAGE_FAILURE);
        }
    }
    private static String canonical(String email) {
        try { return Validation.email(Validation.email(email).toLowerCase(Locale.ROOT)); }
        catch (IllegalArgumentException error) { throw fail(INVALID_INPUT); }
    }
    private static String name(String name) {
        try {
            String value = Validation.text(name,"name");
            if (value.codePointCount(0,value.length()) > 100) throw fail(INVALID_INPUT);
            return value;
        } catch (IllegalArgumentException error) { throw fail(INVALID_INPUT); }
    }
    private static void hash(String hash) { if (hash == null || hash.isBlank()) throw fail(INVALID_INPUT); }
    private static void context(AdminContext admin) { required(admin); required(admin.actorId()); if (admin.expectedGeneration()<0) throw fail(INVALID_INPUT); }
    private static void required(Object... values) { if (Arrays.stream(values).anyMatch(java.util.Objects::isNull)) throw fail(INVALID_INPUT); }
    private static AccountFailure fail(AccountFailure.Code code) { return new AccountFailure(code); }
}
