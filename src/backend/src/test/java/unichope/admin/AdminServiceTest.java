package unichope.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import unichope.platform.auth.AuthenticationService;
import unichope.platform.core.*;
import unichope.platform.web.ApiFailure;

class AdminServiceTest {
  AccountStorage accounts = mock(AccountStorage.class);
  AdminRepository repo = mock(AdminRepository.class);
  AuthenticationService hashes = mock(AuthenticationService.class);
  AdminContext actor = new AdminContext(UUID.randomUUID(), 0);
  AdminService service = new AdminService(accounts, repo, hashes, () -> actor);

  @Test
  void moduleInputsNormalizedAndInvalidNamesCannotReachStorage() {
    service.saveModule(null, " cs3227 ", " Computing ");
    verify(repo).saveModule(actor, null, "CS3227", "Computing");
    clearInvocations(repo);
    for (String code : new String[] {"", "a b", "x".repeat(21), "é"})
      assertThrows(ApiFailure.class, () -> service.saveModule(null, code, "Name"));
    assertThrows(ApiFailure.class, () -> service.saveModule(null, "CS", "bad\nname"));
    verifyNoInteractions(repo);
  }

  @Test
  void adminProvisioningRejectedBeforePasswordHash() {
    assertThrows(ApiFailure.class, () -> service.provision("ADMIN", "Name", "x@y", "password"));
    verifyNoInteractions(hashes, accounts);
  }

  @Test
  void deniedActorStopsRead() {
    var denied =
        new AdminService(
            accounts,
            repo,
            hashes,
            () -> {
              throw new IllegalStateException();
            });
    assertThrows(IllegalStateException.class, denied::statistics);
    verifyNoInteractions(repo);
  }
}
