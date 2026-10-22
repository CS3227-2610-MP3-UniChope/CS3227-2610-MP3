package unichope.student;

import java.util.UUID;

/** Student {@code ModuleOption} JSON: exactly {@code id, code, name} (TECH §3). */
public record ModuleOption(UUID id, String code, String name) {}
