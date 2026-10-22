package unichope.platform.core.postgres;
import static org.junit.jupiter.api.Assertions.*;
import static unichope.platform.core.AccountFailure.Code.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import unichope.platform.core.*;
import unichope.platform.core.model.user.Role;
@Tag("postgres")
class PostgresAccountFailureTest extends AccountPgFixture {
 AdminContext admin() { return new AdminContext(ADMIN,0); }
 @Test void deferredFailureRollsBackAllWritesAndRedactsDetails() {
  db.sql("""
   CREATE FUNCTION reject_commit() RETURNS trigger LANGUAGE plpgsql AS $$
   BEGIN RAISE EXCEPTION 'synthetic-H1' USING ERRCODE='23514', DETAIL=NEW.password_hash; END $$
   """).update();
  db.sql("CREATE CONSTRAINT TRIGGER reject_account AFTER INSERT OR UPDATE ON users DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION reject_commit()").update();
  for(Runnable call:List.<Runnable>of(()->accounts.registerStudent("Synthetic","rollback@local","synthetic-H1"),
    ()->accounts.provisionAccount(admin(),Role.TUTOR,"Synthetic","rollback@local","synthetic-H1"),
    ()->accounts.setAccountActive(admin(),S1,false))) {
   AccountFailure error=assertThrows(AccountFailure.class,call::run); assertEquals(STORAGE_FAILURE,error.code());
   assertNull(error.getCause()); assertFalse(error.toString().contains("synthetic-H1")); assertFalse(error.toString().contains("SQL"));
   assertEquals(5,count("users")); assertTrue(accounts.findAccountState(S1).orElseThrow().user().isActive()); assertEquals(0,accounts.findAccountState(S1).orElseThrow().sessionGeneration());
  }
 }
 @Test void unrelatedUniqueViolationIsStorageFailure() {
  db.sql("CREATE UNIQUE INDEX synthetic_name_unique ON users(name) WHERE name='UniqueSynthetic'").update();
  accounts.registerStudent("UniqueSynthetic","unique1@local","synthetic-H1");
  var failure=assertThrows(AccountFailure.class,()->accounts.registerStudent("UniqueSynthetic","unique2@local","synthetic-H1"));
  assertEquals(STORAGE_FAILURE,failure.code()); assertEquals(6,count("users"));
 }
 @Test void creationBoundsRequiredInputsAndPublicProjection() {
  for(String name:List.of("x","😀".repeat(100),"name\u0001")) assertEquals(name,accounts.registerStudent(" "+name+" ",UUID.randomUUID()+"@local","synthetic-H1").name());
  for(String name:List.of(" ","😀".repeat(101))) assertEquals(INVALID_INPUT,assertThrows(AccountFailure.class,()->accounts.registerStudent(name,"bad@local","synthetic-H1")).code());
  for(String email:List.of("a@"+"x".repeat(253),"@local","a@@local","a\u2003b@local","a\u007fb@local")) assertEquals(INVALID_INPUT,assertThrows(AccountFailure.class,()->accounts.registerStudent("Synthetic",email,"synthetic-H1")).code());
  assertEquals(254,accounts.registerStudent("Synthetic","a@"+"x".repeat(252),"synthetic-H1").email().length());
  for(String hash:new String[]{null,""," \t\n"}) assertEquals(INVALID_INPUT,assertThrows(AccountFailure.class,()->accounts.registerStudent("Synthetic","bad@local",hash)).code());
  assertEquals(INVALID_INPUT,assertThrows(AccountFailure.class,()->accounts.registerStudent(null,"a@local","synthetic-H1")).code());
  assertEquals(INVALID_INPUT,assertThrows(AccountFailure.class,()->accounts.registerStudent("Synthetic",null,"synthetic-H1")).code());
  assertEquals(INVALID_INPUT,assertThrows(AccountFailure.class,()->accounts.provisionAccount(null,Role.STUDENT,"Synthetic","a@local","synthetic-H1")).code());
  assertEquals(INVALID_INPUT,assertThrows(AccountFailure.class,()->accounts.setAccountActive(admin(),null,false)).code());
  var user=new PostgresUserRepository(db).findById(S1).orElseThrow();
  assertFalse(user.toString().contains("synthetic-H1")); assertFalse(accounts.findAccountState(S1).toString().contains("synthetic-H1"));
  assertTrue(accounts.findCredentialByEmail("absent@local").isEmpty()); assertTrue(accounts.findAccountState(UUID.randomUUID()).isEmpty());
 }
 @Test void unicodeCanonicalAndLegacyLookup() {
  String supplied="\u2003İ@LOCALHOST\u2003",canonical="i\u0307@localhost";
  var user=accounts.registerStudent("Synthetic",supplied,"synthetic-H1"); assertEquals(canonical,user.email());
  assertEquals(user,accounts.findCredentialByEmail(canonical).orElseThrow().user());
  var ordinary=new PostgresUserRepository(db); assertEquals(user,ordinary.findByEmail(supplied).orElseThrow());
  db.sql("UPDATE users SET email=' Legacy@LOCALHOST ' WHERE id=:id").param("id",S2).update();
  assertEquals(S2,ordinary.findByEmail("legacy@localhost").orElseThrow().id());
  // The actual collation determines whether DB lower() aliases dotless I to ASCII i.
  String dbFold=db.sql("SELECT lower('I')").query(String.class).single();
  assertNotNull(dbFold);
  db.sql("UPDATE users SET email='ı@localhost' WHERE id=:id").param("id",S1).update();
  var found=ordinary.findByEmail("i@localhost");
  assertTrue(found.isEmpty() || found.orElseThrow().email().strip().toLowerCase(java.util.Locale.ROOT).equals("i@localhost"));
 }
 @Test void collationDependentExtraEquivalenceNeverReturnsWrongCanonicalIdentity() {
  String databaseFold=db.sql("SELECT lower('İ@localhost')").query(String.class).single();
  db.sql("UPDATE users SET email='İ@localhost' WHERE id=:id").param("id",S1).update();
  var ordinary=new PostgresUserRepository(db);
  if (databaseFold.equals("i@localhost")) {
   assertTrue(ordinary.findByEmail("i@localhost").isEmpty());
   assertEquals(ACCOUNT_UNAVAILABLE,assertThrows(AccountFailure.class,()->accounts.registerStudent("Synthetic","i@localhost","synthetic-H1")).code());
  } else {
   var separate=accounts.registerStudent("Synthetic","i@localhost","synthetic-H1");
   assertEquals(separate,ordinary.findByEmail("i@localhost").orElseThrow());
  }
  assertTrue(accounts.findCredentialByEmail("i\u0307@localhost").isEmpty());
 }

}
