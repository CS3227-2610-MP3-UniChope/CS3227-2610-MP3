package unichope.tutor;

import java.util.Optional;
import java.util.UUID;

public interface TutorNoteRepository {
    Optional<TutorNote> findByBookingId(UUID bookingId);
    TutorNote insert(UUID bookingId, String content, long version);
    TutorNote update(UUID bookingId, String content, long version);
}
