package unichope.platform.core;
import java.util.Optional;
import java.util.UUID;
import unichope.platform.core.model.user.Role;
import unichope.platform.core.model.user.User;
/** Core account operations own their short transaction; hash/verify outside core. */
public interface AccountStorage {
    Optional<AccountCredentialSnapshot> findCredentialByEmail(String normalizedEmail);
    Optional<AccountState> findAccountState(UUID userId);
    User registerStudent(String name, String email, String encodedHash);
    User provisionAccount(AdminContext admin, Role role, String name, String email, String encodedHash);
    AccountState setAccountActive(AdminContext admin, UUID targetId, boolean active);
    AccountState confirmVerifiedLogin(UUID userId, Role observedRole, String observedHash, long observedGeneration);
    AccountState changeTutorPassword(UUID userId, long expectedGeneration, String encodedHash);
}
