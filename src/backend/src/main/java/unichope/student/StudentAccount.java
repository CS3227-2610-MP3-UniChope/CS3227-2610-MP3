package unichope.student;

import java.util.UUID;
import unichope.platform.core.model.user.Role;

/** Stored account state used for the per-request Student access check. */
public record StudentAccount(UUID id, Role role, boolean active) {}
