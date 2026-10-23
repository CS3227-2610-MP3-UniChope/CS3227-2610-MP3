package unichope.platform.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AiConfigurationTest {
    @Test
    void taskPoliciesUseTutorSpecificDeadline() {
        var policies = AiConfiguration.taskPolicies(1024);

        assertEquals(Duration.ofSeconds(20), policies.get("tutor.note-draft.v1").deadline());
        assertEquals(Duration.ofSeconds(25), policies.get("STU-AGENDA-1").deadline());
        assertEquals(Duration.ofSeconds(25), policies.get("ADM-REPORT-1").deadline());
    }
}
