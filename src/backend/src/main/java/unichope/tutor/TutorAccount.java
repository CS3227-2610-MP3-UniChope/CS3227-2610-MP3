package unichope.tutor;

import java.util.UUID;
import unichope.platform.core.model.user.Role;

public record TutorAccount(UUID id, Role role, boolean active) {}
