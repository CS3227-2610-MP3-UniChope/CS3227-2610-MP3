package unichope.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import unichope.platform.core.model.user.User;

public final class AdminViews {
  private AdminViews() {}

  public record UserSummary(UUID id, String name, String email, String role, boolean active) {
    public static UserSummary of(User u) {
      return new UserSummary(u.id(), u.name(), u.email(), u.role().name(), u.isActive());
    }
  }

  public record Module(UUID id, String code, String name, boolean active) {}

  public record Assignment(UUID tutorId, UUID moduleId) {}

  public record BookingSummary(
      UUID id,
      UUID studentId,
      String studentName,
      UUID tutorId,
      String tutorName,
      UUID moduleId,
      String moduleCode,
      Instant startTime,
      Instant endTime,
      String status) {}

  public record Group(UUID id, String label, long count) {}

  public record Statistics(
      Instant capturedAt,
      long total,
      long completed,
      long cancelled,
      BigDecimal completionRate,
      BigDecimal cancellationRate,
      List<Group> byModule,
      List<Group> byTutor) {}

  public record Report(
      Instant capturedAt,
      Statistics statistics,
      String draft,
      boolean requiresReview,
      String providerMode) {}
}
