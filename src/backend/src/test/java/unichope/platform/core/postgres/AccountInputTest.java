package unichope.platform.core.postgres;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import unichope.platform.core.AccountFailure;
import unichope.platform.core.AccountCredentialSnapshot;
import unichope.platform.core.model.Validation;
import unichope.platform.core.model.user.Student;
import java.util.UUID;
class AccountInputTest {
 @Test void dotlessAndUnicodeEmailPolicy() {
  assertEquals("a@localhost", Validation.email(" a@localhost "));
  for (String email : new String[]{"a@@b", "@b", "a@", "a b@c", "a\u00a0b@c", "a\u0000@b", "a@"+"x".repeat(253)})
   assertThrows(IllegalArgumentException.class, () -> Validation.email(email));
 }
 @Test void snapshotDiagnosticsAreRedactedAndNotBeanAccessors() {
  var value = new AccountCredentialSnapshot(new Student(UUID.randomUUID(),"Synthetic","a@b",true),"synthetic-hash-sentinel",7);
  assertEquals("synthetic-hash-sentinel", value.encodedHash());
  assertFalse(value.toString().contains("synthetic-hash-sentinel"));
  assertFalse(value.getClass().isRecord());
  assertTrue(java.util.Arrays.stream(value.getClass().getMethods()).noneMatch(m -> m.getName().startsWith("get") && !m.getName().equals("getClass")));
  for (var code : AccountFailure.Code.values()) assertFalse(new AccountFailure(code).toString().contains("synthetic-hash-sentinel"));
 }
}
