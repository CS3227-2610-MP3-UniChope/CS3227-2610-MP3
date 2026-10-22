package unichope.platform.ai;

/** Selects a provider client using a server-owned role key alias, never request data. */
@FunctionalInterface
public interface AiProviderRouter {
  AiGateway providerFor(AiProviderKey key);
}
