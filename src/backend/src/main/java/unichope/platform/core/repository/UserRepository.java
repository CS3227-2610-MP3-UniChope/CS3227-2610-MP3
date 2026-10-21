package unichope.platform.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import unichope.platform.core.model.user.User;

/** Shared reads only; state changes belong to guarded management/lifecycle operations. */
public interface UserRepository {
    Optional<User> findById(UUID id);
    List<User> findAll();
    Optional<User> findByEmail(String email);
}
