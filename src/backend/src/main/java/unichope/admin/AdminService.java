package unichope.admin;

import java.time.LocalDate;
import java.util.*;
import unichope.platform.auth.AuthenticationService;
import unichope.platform.core.*;
import unichope.platform.core.model.user.Role;

public final class AdminService {
  private final AccountStorage accounts;
  private final AdminRepository repository;
  private final AuthenticationService passwords;
  private final AdminActorProvider actors;

  public AdminService(
      AccountStorage accounts,
      AdminRepository repository,
      AuthenticationService passwords,
      AdminActorProvider actors) {
    this.accounts = accounts;
    this.repository = repository;
    this.passwords = passwords;
    this.actors = actors;
  }

  public List<AdminViews.UserSummary> users(String role, Boolean active) {
    actors.currentAdmin();
    if (role != null) role(role);
    return repository.users(role, active);
  }

  public AdminViews.UserSummary provision(String role, String name, String email, String password) {
    var actor = actors.currentAdmin();
    Role r = role(role);
    if (r == Role.ADMIN) throw PostgresAdminRepository.error(403, "FORBIDDEN");
    return AdminViews.UserSummary.of(passwords.provision(actor, r, name, email, password));
  }

  public AdminViews.UserSummary active(UUID id, boolean active) {
    return AdminViews.UserSummary.of(
        accounts.setAccountActive(actors.currentAdmin(), id, active).user());
  }

  public List<AdminViews.Module> modules(Boolean active) {
    actors.currentAdmin();
    return repository.modules(active);
  }

  public AdminViews.Module saveModule(UUID id, String code, String name) {
    var actor = actors.currentAdmin();
    code = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    if (!code.matches("[A-Z0-9-]{1,20}"))
      throw PostgresAdminRepository.error(400, "VALIDATION_FAILED");
    name = text(name, 200);
    return repository.saveModule(actor, id, code, name);
  }

  public AdminViews.Module deactivate(UUID id) {
    return repository.deactivateModule(actors.currentAdmin(), id);
  }

  public List<AdminViews.Assignment> assignments(UUID tutor, UUID module) {
    actors.currentAdmin();
    return repository.assignments(tutor, module);
  }

  public void assign(UUID tutor, UUID module, boolean value) {
    repository.assign(actors.currentAdmin(), tutor, module, value);
  }

  public List<AdminViews.BookingSummary> bookings(
      UUID module, UUID tutor, LocalDate date, String status) {
    actors.currentAdmin();
    if (status != null && !Set.of("ACTIVE", "COMPLETED", "CANCELLED").contains(status))
      throw PostgresAdminRepository.error(400, "VALIDATION_FAILED");
    return repository.bookings(module, tutor, date, status);
  }

  public AdminViews.Statistics statistics() {
    actors.currentAdmin();
    return repository.statistics();
  }

  private static Role role(String raw) {
    try {
      return Role.valueOf(raw);
    } catch (RuntimeException e) {
      throw PostgresAdminRepository.error(400, "VALIDATION_FAILED");
    }
  }

  private static String text(String value, int max) {
    if (value == null) throw PostgresAdminRepository.error(400, "VALIDATION_FAILED");
    String s = value.strip();
    if (s.isEmpty()
        || s.codePointCount(0, s.length()) > max
        || s.codePoints().anyMatch(Character::isISOControl))
      throw PostgresAdminRepository.error(400, "VALIDATION_FAILED");
    return s;
  }
}
