package unichope.tutor.http;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import unichope.platform.ai.AiGateway;

class TutorAiFailureMappingTest {
    @Test
    void mapsEveryAiFailureToItsStableSanitizedHttpContract() throws Exception {
        Map<AiGateway.FailureCode, Expected> expected = Map.of(
                AiGateway.FailureCode.BUSY, new Expected(409, "AI_BUSY"),
                AiGateway.FailureCode.RATE_LIMITED, new Expected(429, "AI_RATE_LIMITED"),
                AiGateway.FailureCode.UNAVAILABLE, new Expected(503, "AI_UNAVAILABLE"),
                AiGateway.FailureCode.TIMEOUT, new Expected(504, "AI_TIMEOUT"),
                AiGateway.FailureCode.INVALID_OUTPUT, new Expected(502, "AI_INVALID_OUTPUT"));

        for (var entry : expected.entrySet()) {
            var request = new MockHttpServletRequest();
            var response = new MockHttpServletResponse();

            new TutorExceptionHandler().handle(new AiGateway.Failure(entry.getKey()), request, response);

            assertEquals(entry.getValue().status(), response.getStatus(), entry.getKey().name());
            assertTrue(response.getContentAsString().contains("\"code\":\"" + entry.getValue().code() + "\""));
            assertFalse(response.getContentAsString().contains("provider"));
            assertFalse(response.getContentAsString().contains("stack"));
            assertNull(response.getHeader("Retry-After"));
        }
    }

    @Test
    void busyAndRateLimitedFailuresRoundRetryAfterUpToWholeSeconds() throws Exception {
        for (var code : new AiGateway.FailureCode[] {
                AiGateway.FailureCode.BUSY, AiGateway.FailureCode.RATE_LIMITED}) {
            var request = new MockHttpServletRequest();
            var response = new MockHttpServletResponse();

            new TutorExceptionHandler().handle(
                    new AiGateway.Failure(code, Duration.ofMillis(1500)), request, response);

            assertEquals("2", response.getHeader("Retry-After"), code.name());
            assertTrue(response.getContentAsString().contains("\"code\":\"AI_"));
            assertFalse(response.getContentAsString().contains("1500"));
        }
    }

    private record Expected(int status, String code) {}
}
