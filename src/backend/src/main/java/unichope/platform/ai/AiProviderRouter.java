package unichope.platform.ai;

/**
 * Resolves a configured provider client using a server-owned role key alias, never
 * request data. Resolution must not contact the provider; the admitted gateway
 * performs it before quota admission so an unavailable route consumes no quota.
 */
@FunctionalInterface
public interface AiProviderRouter {
  AiGateway providerFor(AiProviderKey key);
}
