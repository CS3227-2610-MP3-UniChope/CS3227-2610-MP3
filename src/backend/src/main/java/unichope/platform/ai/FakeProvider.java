package unichope.platform.ai;

import java.util.Objects;

/**
 * Deterministic isolated dev/test double. Each role supplies its synthetic fixture;
 * no prompt interpretation, credentials, network, persistence, or Spring bean wiring.
 */
public final class FakeProvider implements AiGateway {
    private final Result result;

    public FakeProvider(String fixture) {
        result = new Result(Objects.requireNonNull(fixture), ProviderMode.FAKE);
    }

    @Override
    public Result generate(Request request) {
        Objects.requireNonNull(request);
        return result;
    }
}
