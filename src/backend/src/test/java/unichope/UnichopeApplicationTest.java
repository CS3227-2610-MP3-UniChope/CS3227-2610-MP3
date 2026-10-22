package unichope;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import unichope.platform.ai.AiGateway;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UnichopeApplicationTest {
    @Autowired
    private ApplicationContext context;

    @Test
    void scaffoldStartsWithoutExposingAnUnprotectedGateway() {
        assertTrue(context.getBeansOfType(AiGateway.class).isEmpty());
    }
}
