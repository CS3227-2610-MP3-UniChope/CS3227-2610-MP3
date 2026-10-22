package unichope.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import unichope.platform.core.AdminContext;

public interface AdminRepository {
  List<AdminViews.UserSummary> users(String role, Boolean active);

  List<AdminViews.Module> modules(Boolean active);

  AdminViews.Module saveModule(AdminContext actor, UUID id, String code, String name);

  AdminViews.Module deactivateModule(AdminContext actor, UUID id);

  List<AdminViews.Assignment> assignments(UUID tutorId, UUID moduleId);

  void assign(AdminContext actor, UUID tutorId, UUID moduleId, boolean assigned);

  List<AdminViews.BookingSummary> bookings(
      UUID moduleId, UUID tutorId, LocalDate date, String status);

  AdminViews.Statistics statistics();
}
