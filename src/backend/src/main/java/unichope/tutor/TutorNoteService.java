package unichope.tutor;

import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;
import unichope.platform.core.ConsultationLifecycle;
import unichope.platform.core.LifecycleFailure;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.model.consultation.SlotStatus;
import unichope.platform.core.repository.BookingRepository;
import unichope.platform.core.repository.SlotRepository;

public final class TutorNoteService {
    private static final Set<String> CONTENTION_STATES = Set.of("55P03", "40P01", "40001", "57014");
    private final TutorAccess access;
    private final BookingRepository bookings;
    private final SlotRepository slots;
    private final TutorNoteRepository notes;
    private final ConsultationLifecycle lifecycle;
    private final TransactionTemplate transaction;

    public TutorNoteService(TutorAccess access, BookingRepository bookings, SlotRepository slots,
            TutorNoteRepository notes, ConsultationLifecycle lifecycle, DataSource source) {
        this.access = access;
        this.bookings = bookings;
        this.slots = slots;
        this.notes = notes;
        this.lifecycle = lifecycle;
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.transaction.setTimeout(10);
    }

    public TutorNote load(UUID bookingId) {
        UUID tutorId = access.requireActiveTutor();
        authorizeRead(tutorId, bookingId);
        return notes.findByBookingId(bookingId).orElse(new TutorNote(bookingId, null, 0, null));
    }

    public TutorNote save(UUID bookingId, String rawContent, Long expectedVersion) {
        UUID tutorId = access.requireActiveTutor();
        String content = normalizeContent(rawContent);
        if (expectedVersion == null || expectedVersion < 0) throw new TutorNoteFailure(TutorNoteFailure.Code.INVALID_CONTENT);
        try {
            return transaction.execute(status -> {
                lifecycle.lockForPrivateNote(tutorId, bookingId);
                var current = notes.findByBookingId(bookingId);
                long actualVersion = current.map(TutorNote::version).orElse(0L);
                if (actualVersion != expectedVersion) throw new TutorNoteFailure(TutorNoteFailure.Code.NOTE_CONFLICT);
                return current.isEmpty()
                        ? notes.insert(bookingId, content, 1)
                        : notes.update(bookingId, content, actualVersion + 1);
            });
        } catch (LifecycleFailure failure) {
            if (failure.code() == LifecycleFailure.Code.ACCESS_DENIED) {
                access.requireActiveTutor();
                throw new TutorAccessDeniedException(TutorAccessDeniedException.Reason.FORBIDDEN);
            }
            throw failure;
        } catch (DataAccessException failure) {
            if (isContention(failure)) throw new TutorNoteFailure(TutorNoteFailure.Code.TEMPORARILY_UNAVAILABLE);
            throw failure;
        } catch (TransactionSystemException failure) {
            if (isContention(failure)) throw new TutorNoteFailure(TutorNoteFailure.Code.TEMPORARILY_UNAVAILABLE);
            throw failure;
        }
    }

    private void authorizeRead(UUID tutorId, UUID bookingId) {
        var booking = bookings.findById(bookingId)
                .orElseThrow(() -> new LifecycleFailure(LifecycleFailure.Code.NOT_FOUND));
        var slot = slots.findById(booking.slotId())
                .orElseThrow(() -> new LifecycleFailure(LifecycleFailure.Code.NOT_FOUND));
        if (!slot.tutorId().equals(tutorId)) throw new LifecycleFailure(LifecycleFailure.Code.NOT_FOUND);
        if (booking.status() != BookingStatus.COMPLETED || slot.status() != SlotStatus.COMPLETED) {
            throw new LifecycleFailure(LifecycleFailure.Code.INVALID_STATE);
        }
    }

    static String normalizeContent(String value) {
        if (value == null || value.codePointCount(0, value.length()) < 1 || value.codePointCount(0, value.length()) > 8000) {
            throw new TutorNoteFailure(TutorNoteFailure.Code.INVALID_CONTENT);
        }
        String stripped = value.strip();
        if (stripped.isEmpty()) throw new TutorNoteFailure(TutorNoteFailure.Code.INVALID_CONTENT);
        return stripped;
    }

    private static boolean isContention(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && CONTENTION_STATES.contains(sql.getSQLState())) return true;
        }
        return false;
    }
}
