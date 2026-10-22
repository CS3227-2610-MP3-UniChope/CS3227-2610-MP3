package unichope.platform.runtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;
import unichope.admin.*;
import unichope.platform.ai.AiGateway;

class RuntimePolicyTest {
  @Test
  void productionCannotEnableFakeReports() {
    var environment = new MockEnvironment().withProperty("unichope.ai.mode", "fake");
    environment.setActiveProfiles("production");
    var beans = new StaticListableBeanFactory();
    assertThrows(
        IllegalStateException.class,
        () ->
            new AdminConfiguration()
                .activityReportService(
                    mock(AdminActorProvider.class),
                    mock(AdminRepository.class),
                    environment,
                    Clock.systemUTC(),
                    beans.getBeanProvider(AiGateway.class)));
  }

  @Test
  void selectingSharedModeWithoutGatewayFailsClosed() {
    var environment = new MockEnvironment().withProperty("unichope.ai.mode", "shared");
    var beans = new StaticListableBeanFactory();
    assertThrows(
        IllegalStateException.class,
        () ->
            new AdminConfiguration()
                .activityReportService(
                    mock(AdminActorProvider.class),
                    mock(AdminRepository.class),
                    environment,
                    Clock.systemUTC(),
                    beans.getBeanProvider(AiGateway.class)));
  }
}
