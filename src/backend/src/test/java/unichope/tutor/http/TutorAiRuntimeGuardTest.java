package unichope.tutor.http;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import unichope.platform.ai.AiGateway;

class TutorAiRuntimeGuardTest {
    private static final String VALID_OUTPUT =
            "{\"summary\":\"Summary\",\"topicsDiscussed\":\"Topics\",\"followUp\":\"Next\"}";

    @Test
    void fakeProviderIsUnavailableOutsideLocalProfile() {
        AiGateway fake = request -> new AiGateway.Result(VALID_OUTPUT, AiGateway.ProviderMode.FAKE);
        var environment = new MockEnvironment().withProperty("spring.profiles.active", "production");
        var tutorGateway = new TutorApiConfiguration().tutorNoteDraftGateway(fake, environment, 256);

        var failure = assertThrows(AiGateway.Failure.class,
                () -> tutorGateway.generate(UUID.randomUUID(), "rough notes"));

        assertEquals(AiGateway.FailureCode.UNAVAILABLE, failure.code());
    }

    @Test
    void rawProductionProviderCannotBypassSharedAdmission() {
        var calls = new AtomicInteger();
        AiGateway rawProvider = request -> {
            calls.incrementAndGet();
            return new AiGateway.Result(VALID_OUTPUT, AiGateway.ProviderMode.SOC);
        };
        var environment = new MockEnvironment().withProperty("spring.profiles.active", "production");
        var tutorGateway = new TutorApiConfiguration().tutorNoteDraftGateway(rawProvider, environment, 256);

        var failure = assertThrows(AiGateway.Failure.class,
                () -> tutorGateway.generate(UUID.randomUUID(), "rough notes"));

        assertEquals(AiGateway.FailureCode.UNAVAILABLE, failure.code());
        assertEquals(0, calls.get());
    }

    @Test
    void fakeProviderRemainsAvailableInLocalProfileAndKeepsItsLabel() {
        AiGateway fake = request -> new AiGateway.Result(VALID_OUTPUT, AiGateway.ProviderMode.FAKE);
        var environment = new MockEnvironment().withProperty("spring.profiles.active", "local");
        var tutorGateway = new TutorApiConfiguration().tutorNoteDraftGateway(fake, environment, 256);

        var result = tutorGateway.generate(UUID.randomUUID(), "rough notes");

        assertEquals(AiGateway.ProviderMode.FAKE, result.providerMode());
    }
}
