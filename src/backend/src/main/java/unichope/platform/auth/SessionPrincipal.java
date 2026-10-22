package unichope.platform.auth;

import java.io.Serializable;
import java.util.UUID;

public record SessionPrincipal(UUID userId, long generation) implements Serializable {
  private static final long serialVersionUID = 1L;
}
