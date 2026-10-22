package unichope.platform.ai;

import unichope.platform.core.model.user.Role;

/** One SoC API key, budget and quota row per role. Requests never borrow another slot's key. */
public enum AiKeySlot {
  STUDENT(Role.STUDENT),
  TUTOR(Role.TUTOR),
  ADMIN(Role.ADMIN);

  private final Role role;

  AiKeySlot(Role role) {
    this.role = role;
  }

  public Role role() {
    return role;
  }
}
