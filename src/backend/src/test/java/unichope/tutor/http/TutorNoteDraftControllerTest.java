package unichope.tutor.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import unichope.platform.ai.AiGateway;
import unichope.platform.auth.SessionPrincipal;
import unichope.platform.web.ApiFailure;
import unichope.tutor.TutorNoteDraftService;
import unichope.tutor.TutorNoteService;
import unichope.tutor.TutorReadService;

class TutorNoteDraftControllerTest {
    private static final UUID BOOKING = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TUTOR = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void returnsTheReviewedDraftContractFromService() throws Exception {
        var service = mock(TutorNoteDraftService.class);
        when(service.generate(new SessionPrincipal(TUTOR, 4), BOOKING, "rough notes"))
                .thenReturn(new TutorNoteDraftService.Result(BOOKING, "draft", AiGateway.ProviderMode.FAKE));
        var factory = new StaticListableBeanFactory();
        factory.addBean("draftService", service);
        var controller = controller(factory.getBeanProvider(TutorNoteDraftService.class));

        var result = controller.draftNote(BOOKING.toString(), json("{\"roughNotes\":\"rough notes\"}"),
                new SessionPrincipal(TUTOR, 4));

        assertEquals(new TutorController.TutorNoteDraftResponse(BOOKING, "draft", true, "fake"), result);
        verify(service).generate(new SessionPrincipal(TUTOR, 4), BOOKING, "rough notes");
    }

    @Test
    void rejectsInvalidBodyAndMissingRuntimeGatewayWithoutServiceCalls() throws Exception {
        var service = mock(TutorNoteDraftService.class);
        var factory = new StaticListableBeanFactory();
        factory.addBean("draftService", service);
        var controller = controller(factory.getBeanProvider(TutorNoteDraftService.class));
        var principal = new SessionPrincipal(TUTOR, 4);

        var invalid = assertThrows(ApiFailure.class, () -> controller.draftNote(
                BOOKING.toString(), json("{\"roughNotes\":\" \",\"extra\":true}"), principal));
        assertEquals(400, invalid.status());
        assertEquals("VALIDATION_FAILED", invalid.code());
        verifyNoInteractions(service);

        var unavailableFactory = new StaticListableBeanFactory();
        var unavailableController = controller(unavailableFactory.getBeanProvider(TutorNoteDraftService.class));
        var unavailable = assertThrows(ApiFailure.class, () -> unavailableController.draftNote(
                BOOKING.toString(), json("{\"roughNotes\":\"rough notes\"}"), principal));
        assertEquals(503, unavailable.status());
        assertEquals("AI_UNAVAILABLE", unavailable.code());
        verifyNoInteractions(service);
    }

    private static TutorController controller(ObjectProvider<TutorNoteDraftService> draftService) {
        return new TutorController(mock(TutorReadService.class), mock(TutorHttpService.class),
                mock(TutorNoteService.class), draftService);
    }

    private static MockHttpServletRequest json(String body) {
        var request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
