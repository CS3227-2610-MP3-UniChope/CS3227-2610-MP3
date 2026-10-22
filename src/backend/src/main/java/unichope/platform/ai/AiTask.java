package unichope.platform.ai;

import java.util.Arrays;
import java.util.Optional;

/**
 * Server-defined AI tasks. The task id a role service sends selects the key slot; nothing from
 * the browser does. Unknown task ids are refused before any quota or provider use.
 */
public enum AiTask {
  STUDENT_AGENDA("STU-AGENDA-1", AiKeySlot.STUDENT),
  TUTOR_NOTE_DRAFT("TUT-NOTE-1", AiKeySlot.TUTOR),
  ADMIN_ACTIVITY_REPORT("ADM-REPORT-1", AiKeySlot.ADMIN);

  private final String id;
  private final AiKeySlot slot;

  AiTask(String id, AiKeySlot slot) {
    this.id = id;
    this.slot = slot;
  }

  public String id() {
    return id;
  }

  public AiKeySlot slot() {
    return slot;
  }

  public static Optional<AiTask> fromId(String id) {
    return Arrays.stream(values()).filter(task -> task.id.equals(id)).findFirst();
  }
}
