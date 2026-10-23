package unichope.platform.runtime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import unichope.platform.auth.SessionPrincipal;
import unichope.platform.ai.AiGateway;
import unichope.platform.core.model.user.Role;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.student.*;
class AgendaSessionProbeTest {
 @Test void actualRuntimeActorAdapterStillReturnsAgendaAfterOriginalSessionInvalidation() {
  UUID actor=UUID.randomUUID(), booking=UUID.randomUUID();
  var session=new MockHttpSession();
  var principal=new SessionPrincipal(actor,0);
  var context=SecurityContextHolder.createEmptyContext();
  context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,List.of()));
  SecurityContextHolder.setContext(context);
  session.setAttribute("SPRING_SECURITY_CONTEXT",context);
  StudentReadRepository reads=mock(StudentReadRepository.class);
  when(reads.findAccount(actor)).thenReturn(Optional.of(new StudentAccount(actor,Role.STUDENT,true)));
  var row=new StudentReadRepository.BookingRow(booking,UUID.randomUUID(),UUID.randomUUID(),"CS3227","Architecture","Synthetic",Instant.parse("2099-01-01T00:00:00Z"),Instant.parse("2099-01-01T01:00:00Z"),BookingStatus.ACTIVE);
  when(reads.findStudentBooking(actor,booking)).thenReturn(Optional.of(row));
  var access=new StudentAccess(new ApplicationConfiguration().sharedActor(),reads);
  AiGateway gateway=request->{session.invalidate();return new AiGateway.Result("{\"goal\":\"Discuss topic\",\"items\":[{\"topic\":\"Topic\",\"question\":\"Clarify topic\",\"minutes\":1}],\"prepare\":[]}",AiGateway.ProviderMode.FAKE);};
  try {
   var result=new AgendaService(access,reads,Clock.fixed(Instant.parse("2026-10-23T00:00:00Z"),ZoneOffset.UTC),()->gateway).generate(booking,"topic");
   assertTrue(session.isInvalid()); assertTrue(result.agenda().contains("Discuss topic"));
   System.out.println("CONFIRMED: actual runtime actor adapter and service returned agenda after original session invalidation");
  } finally {SecurityContextHolder.clearContext();}
 }
}
