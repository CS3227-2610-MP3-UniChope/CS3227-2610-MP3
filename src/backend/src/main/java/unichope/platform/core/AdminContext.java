package unichope.platform.core;
import java.util.UUID;
/** Trusted auth handoff, never populated directly from browser identity fields. */
public record AdminContext(UUID actorId, long expectedGeneration) { }
