package unichope.platform.core.postgres;
import static org.junit.jupiter.api.Assertions.*;
import static unichope.platform.core.AccountFailure.Code.*;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import unichope.platform.core.*;
import unichope.platform.core.model.user.Role;
@Tag("postgres")
class PostgresAccountConcurrencyTest extends AccountPgFixture {
 AdminContext admin() { return new AdminContext(ADMIN,0); }
 void code(Object value, AccountFailure.Code expected) { assertInstanceOf(AccountFailure.class,value); assertEquals(expected,((AccountFailure)value).code()); }
 @Test void repeatedDeactivationIncrementsOnce() throws Exception {
  var outcomes=race(S1,()->accounts.setAccountActive(admin(),S1,false),()->accounts.setAccountActive(admin(),S1,false));
  for(Object outcome:outcomes) assertEquals(1,((AccountState)outcome).sessionGeneration());
  assertEquals(1,accounts.findAccountState(S1).orElseThrow().sessionGeneration());
 }
 @Test void duplicateRegistrationAndProvisionRaces() throws Exception {
  for(boolean provision:List.of(false,true)) {
   String email="race"+provision+"@local";
   try(Connection holder=source.getConnection()) {
    holder.setAutoCommit(false);
    on(holder).sql("INSERT INTO users (id,name,email,role,active,password_hash) VALUES (:id,'Synthetic',:email,'STUDENT',true,'synthetic-H0')").param("id",UUID.randomUUID()).param("email",email).update();
    Future<Object> first=launch(()->accounts.registerStudent("First",email.toUpperCase(java.util.Locale.ROOT),"synthetic-H1")); awaitBlocked(1);
    Future<Object> second=launch(()-> provision ? accounts.provisionAccount(admin(),Role.TUTOR,"Second",email,"synthetic-H2") : accounts.registerStudent("Second",email,"synthetic-H2")); awaitBlocked(2);
    holder.rollback();
    var outcomes=List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS));
    assertEquals(1,outcomes.stream().filter(AccountFailure.class::isInstance).count());
    outcomes.stream().filter(AccountFailure.class::isInstance).forEach(x->code(x,ACCOUNT_UNAVAILABLE));
    var c=accounts.findCredentialByEmail(email).orElseThrow();
    assertEquals(c.user().name().equals("First")?"synthetic-H1":"synthetic-H2",c.encodedHash()); assertEquals(0,c.sessionGeneration());
   }
  }
 }
 @Test void studentBookingAndDeactivationBothOrders() throws Exception {
  for(boolean bookingFirst:List.of(true,false)) {
   UUID slot=slot(T1,START,END,"AVAILABLE");
   var result=bookingFirst ? race(S1,()->lifecycle.bookAvailableSlot(S1,slot),()->accounts.setAccountActive(admin(),S1,false))
     : race(S1,()->accounts.setAccountActive(admin(),S1,false),()-> { try { return lifecycle.bookAvailableSlot(S1,slot); } catch(LifecycleFailure failure) { return failure; } });
   if(bookingFirst) { code(result.get(1),INVALID_STATE); assertEquals(1,count("bookings")); }
   else { assertInstanceOf(LifecycleFailure.class,result.get(1)); assertEquals(LifecycleFailure.Code.ACCESS_DENIED,((LifecycleFailure)result.get(1)).code()); assertEquals(0,count("bookings")); }
   clearConsultations(); accounts.setAccountActive(admin(),S1,true);
  }
 }
 @Test void tutorSlotCreationBothOrders() throws Exception {
  for(boolean createFirst:List.of(true,false)) {
   var result=createFirst ? race(T1,()->lifecycle.createSlot(T1,M,START,END),()->accounts.setAccountActive(admin(),T1,false))
     : race(T1,()->accounts.setAccountActive(admin(),T1,false),()-> { try { return lifecycle.createSlot(T1,M,START,END); } catch(LifecycleFailure failure) { return failure; } });
   if(createFirst) { code(result.get(1),INVALID_STATE); assertEquals(1,count("slots")); }
   else { assertInstanceOf(LifecycleFailure.class,result.get(1)); assertEquals(LifecycleFailure.Code.ACCESS_DENIED,((LifecycleFailure)result.get(1)).code()); assertEquals(0,count("slots")); }
   clearConsultations(); accounts.setAccountActive(admin(),T1,true);
  }
 }
 @Test void tutorBookingRaceDeniesDeactivationInBothOrders() throws Exception {
  for(boolean bookingFirst:List.of(true,false)) {
   UUID slot=slot(T1,START,END,"AVAILABLE");
   var result=bookingFirst ? race(T1,()->lifecycle.bookAvailableSlot(S1,slot),()->accounts.setAccountActive(admin(),T1,false))
     : race(T1,()->accounts.setAccountActive(admin(),T1,false),()->lifecycle.bookAvailableSlot(S1,slot));
   code(result.get(bookingFirst?1:0),INVALID_STATE); assertEquals(1,count("bookings")); assertTrue(accounts.findAccountState(T1).orElseThrow().user().isActive()); clearConsultations();
  }
 }
 @Test void clockIsReadAfterLockWait() throws Exception {
  slot(T1,START,END,"AVAILABLE");
  try(Connection holder=holder(T1)) {
   var pending=launch(()->accounts.setAccountActive(admin(),T1,false)); awaitBlocked(1); clock.now.set(START); holder.commit();
   assertFalse(((AccountState)pending.get(15,TimeUnit.SECONDS)).user().isActive());
  }
 }
 @Test void staleActorAndConfirmationRecheckedAfterLockWait() throws Exception {
  try(Connection holder=holder(ADMIN)) {
   var pending=launch(()->accounts.provisionAccount(admin(),Role.STUDENT,"Synthetic","stale@local","synthetic-H1")); awaitBlocked(1);
   on(holder).sql("UPDATE users SET session_generation=1 WHERE id=:id").param("id",ADMIN).update(); holder.commit(); code(pending.get(15,TimeUnit.SECONDS),ACCESS_DENIED);
  }
  for(String update:List.of("role='TUTOR'","password_hash='synthetic-H2'","session_generation=session_generation+1","active=false")) {
   db.sql("UPDATE users SET role='STUDENT',password_hash='synthetic-H1',session_generation=0,active=true WHERE id=:id").param("id",S1).update();
   try(Connection holder=holder(S1)) {
    var pending=launch(()->accounts.confirmVerifiedLogin(S1,Role.STUDENT,"synthetic-H1",0)); awaitBlocked(1);
    on(holder).sql("UPDATE users SET "+update+" WHERE id=:id").param("id",S1).update(); holder.commit(); code(pending.get(15,TimeUnit.SECONDS),LOGIN_REJECTED);
   }
  }
 }
 @Test void confirmationThenRevocationRequiresFreshAuthCheck() {
  var confirmed=accounts.confirmVerifiedLogin(S1,Role.STUDENT,"synthetic-H1",0);
  accounts.setAccountActive(admin(),S1,false); accounts.setAccountActive(admin(),S1,true);
  assertTrue(accounts.findAccountState(S1).orElseThrow().user().isActive());
  assertNotEquals(confirmed.sessionGeneration(),accounts.findAccountState(S1).orElseThrow().sessionGeneration());
 }
 @Test void timeoutAndFreshTransactionRecovery() throws Exception {
  for(int operation=0;operation<3;operation++) {
   final int selected=operation;
   UUID held=selected==0?ADMIN:S1;
   try(Connection holder=holder(held)) {
    var pending=launch(()->switch(selected) {
     case 0 -> accounts.provisionAccount(admin(),Role.STUDENT,"Synthetic","timeout@local","synthetic-H1");
     case 1 -> accounts.setAccountActive(admin(),S1,false);
     default -> accounts.confirmVerifiedLogin(S1,Role.STUDENT,"synthetic-H1",0);
    }); awaitBlocked(1); code(pending.get(12,TimeUnit.SECONDS),TEMPORARILY_UNAVAILABLE); holder.rollback();
   }
   assertTrue(accounts.confirmVerifiedLogin(S1,Role.STUDENT,"synthetic-H1",0).user().isActive());
  }
 }
}
