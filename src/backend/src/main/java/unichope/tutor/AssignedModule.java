package unichope.tutor;

import java.util.UUID;

public record AssignedModule(UUID id, String code, String name, boolean active) {}
