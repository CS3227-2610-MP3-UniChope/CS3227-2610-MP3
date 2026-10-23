package unichope.platform.core.postgres;
import static org.junit.jupiter.api.Assertions.*;
import static unichope.platform.core.AccountFailure.Code.*;
import java.util.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import unichope.platform.core.*;
import unichope.platform.core.model.user.*;
@Tag("postgres")
class PostgresAccountStorageTest extends AccountPgFixture {
 AdminContext admin() { return new AdminContext(ADMIN,0); }
 void denied(AccountFailure.Code code,Runnable call) { assertEquals(code,assertThrows(AccountFailure.class,call::run).code()); }
 @Test void registerIsAtomicCanonicalAndSafe() {
  User user=accounts.registerStudent(" Synthetic "," New@localhost ","synthetic-H2");
  assertEquals(Role.STUDENT,user.role()); assertTrue(user.isActive()); assertEquals("new@localhost",user.email());
  var credential=accounts.findCredentialByEmail("new@localhost").orElseThrow();
  assertEquals("synthetic-H2",credential.encodedHash()); assertEquals(0,credential.sessionGeneration());
  assertEquals(user,accounts.findAccountState(user.id()).orElseThrow().user());
  denied(ACCOUNT_UNAVAILABLE,()->accounts.registerStudent("Duplicate"," NEW@LOCALHOST ","synthetic-H1"));
  assertEquals(6,count("users"));
 }
 @Test void guardsProvisioningAndTargetState() {
  for(Role role:List.of(Role.STUDENT,Role.TUTOR)) assertEquals(role,accounts.provisionAccount(admin(),role,"Synthetic",role+"@local","synthetic-H2").role());
  denied(ACCESS_DENIED,()->accounts.provisionAccount(admin(),Role.ADMIN,"Synthetic","admin2@local","synthetic-H2"));
  denied(INVALID_INPUT,()->accounts.provisionAccount(admin(),null,"Synthetic","null@local","synthetic-H2"));
  for(UUID actor:List.of(S1,T1,UUID.randomUUID())) denied(ACCESS_DENIED,()->accounts.setAccountActive(new AdminContext(actor,0),UUID.randomUUID(),false));
  denied(ACCESS_DENIED,()->accounts.setAccountActive(new AdminContext(ADMIN,1),S1,false));
  denied(NOT_FOUND,()->accounts.setAccountActive(admin(),UUID.randomUUID(),false));
  denied(ACCESS_DENIED,()->accounts.setAccountActive(admin(),ADMIN,true));
 }
 @Test void tutorProvisioningRequiresOneAtomicPasswordChangeAndRevokesOldGeneration() {
  User created=accounts.provisionAccount(admin(),Role.TUTOR,"New tutor","new-tutor@local","synthetic-H2");
  var first=accounts.findAccountState(created.id()).orElseThrow();
  assertTrue(first.mustChangePassword());
  var changed=accounts.changeTutorPassword(created.id(),first.sessionGeneration(),"synthetic-H3");
  assertFalse(changed.mustChangePassword()); assertEquals(1,changed.sessionGeneration());
  assertEquals("synthetic-H3",accounts.findCredentialByEmail(created.email()).orElseThrow().encodedHash());
  denied(ACCESS_DENIED,()->accounts.changeTutorPassword(created.id(),0,"synthetic-H4"));
  denied(ACCESS_DENIED,()->accounts.changeTutorPassword(S1,0,"synthetic-H4"));
  assertFalse(accounts.findAccountState(S1).orElseThrow().mustChangePassword());
 }
 @Test void concurrentChangesCannotReuseTheInitialSessionGeneration() throws Exception {
  User created=accounts.provisionAccount(admin(),Role.TUTOR,"New tutor","race-tutor@local","synthetic-H2");
  var ready=new java.util.concurrent.CountDownLatch(2); var start=new java.util.concurrent.CountDownLatch(1);
  var first=launch(()->{ready.countDown(); start.await(); return accounts.changeTutorPassword(created.id(),0,"synthetic-H3");});
  var second=launch(()->{ready.countDown(); start.await(); return accounts.changeTutorPassword(created.id(),0,"synthetic-H4");});
  assertTrue(ready.await(5,java.util.concurrent.TimeUnit.SECONDS)); start.countDown();
  Object a=first.get(15,java.util.concurrent.TimeUnit.SECONDS), b=second.get(15,java.util.concurrent.TimeUnit.SECONDS);
  assertEquals(1,java.util.stream.Stream.of(a,b).filter(AccountState.class::isInstance).count());
  assertEquals(1,java.util.stream.Stream.of(a,b).filter(AccountFailure.class::isInstance).count());
  var finalState=accounts.findAccountState(created.id()).orElseThrow();
  assertFalse(finalState.mustChangePassword()); assertEquals(1,finalState.sessionGeneration());
 }
 @Test void generationAndOverflow() {
  assertEquals(1,accounts.setAccountActive(admin(),S1,false).sessionGeneration());
  assertEquals(1,accounts.setAccountActive(admin(),S1,false).sessionGeneration());
  assertEquals(1,accounts.setAccountActive(admin(),S1,true).sessionGeneration());
  assertEquals(1,accounts.setAccountActive(admin(),S1,true).sessionGeneration());
  assertEquals(2,accounts.setAccountActive(admin(),S1,false).sessionGeneration());
  db.sql("UPDATE users SET session_generation=9223372036854775807 WHERE id=:id").param("id",T1).update();
  denied(INVALID_STATE,()->accounts.setAccountActive(admin(),T1,false));
  assertTrue(accounts.findAccountState(T1).orElseThrow().user().isActive());
 }
 @Test void blockersRetainHistoryAndReactivationWorks() {
  for(long seconds:List.of(-7200L,0L,3600L)) {
   UUID slot=slot(T1,N.plusSeconds(seconds),N.plusSeconds(seconds+3600),"BOOKED"); booking(S1,slot,"ACTIVE");
   denied(INVALID_STATE,()->accounts.setAccountActive(admin(),S1,false)); denied(INVALID_STATE,()->accounts.setAccountActive(admin(),T1,false)); clearConsultations();
  }
  slot(T1,START,END,"AVAILABLE"); denied(INVALID_STATE,()->accounts.setAccountActive(admin(),T1,false));
  db.sql("UPDATE users SET active=false WHERE id=:id").param("id",T1).update();
  assertTrue(accounts.setAccountActive(admin(),T1,true).user().isActive()); clearConsultations();
  slot(T1,N,N.plusSeconds(3600),"AVAILABLE"); assertFalse(accounts.setAccountActive(admin(),T1,false).user().isActive()); assertEquals(1,count("slots")); assertEquals(4,count("tutor_modules"));
 }
 @Test void confirmationChecksEveryObservedDimension() {
  var c=accounts.findCredentialByEmail(S1+"@example.edu").orElseThrow();
  assertEquals(S1,accounts.confirmVerifiedLogin(S1,Role.STUDENT,c.encodedHash(),0).user().id());
  denied(LOGIN_REJECTED,()->accounts.confirmVerifiedLogin(S1,Role.TUTOR,c.encodedHash(),0));
  denied(LOGIN_REJECTED,()->accounts.confirmVerifiedLogin(S1,Role.STUDENT,"synthetic-H2",0));
  denied(LOGIN_REJECTED,()->accounts.confirmVerifiedLogin(S1,Role.STUDENT,c.encodedHash(),1));
  denied(LOGIN_REJECTED,()->accounts.confirmVerifiedLogin(UUID.randomUUID(),Role.STUDENT,c.encodedHash(),0));
  accounts.setAccountActive(admin(),S1,false); denied(LOGIN_REJECTED,()->accounts.confirmVerifiedLogin(S1,Role.STUDENT,c.encodedHash(),0));
  accounts.setAccountActive(admin(),S1,true); denied(LOGIN_REJECTED,()->accounts.confirmVerifiedLogin(S1,Role.STUDENT,c.encodedHash(),0));
 }
 @Test void historicalBookingsDoNotBlockAndInactiveAdminCannotProvision() {
  for(String status:List.of("CANCELLED","COMPLETED")) {
   UUID slot=slot(T1,START,END,status.equals("COMPLETED")?"COMPLETED":"CANCELLED"); booking(S1,slot,status);
   assertFalse(accounts.setAccountActive(admin(),S1,false).user().isActive());
   assertFalse(accounts.setAccountActive(admin(),T1,false).user().isActive());
   assertEquals(1,count("bookings")); assertEquals(1,count("slots"));
   accounts.setAccountActive(admin(),S1,true); accounts.setAccountActive(admin(),T1,true); clearConsultations();
  }
  for(UUID actor:List.of(S1,T1,UUID.randomUUID())) denied(ACCESS_DENIED,()->accounts.provisionAccount(new AdminContext(actor,0),Role.STUDENT,"Synthetic","forbidden@local","synthetic-H1"));
  db.sql("UPDATE users SET active=false WHERE id=:id").param("id",ADMIN).update();
  denied(ACCESS_DENIED,()->accounts.provisionAccount(admin(),Role.STUDENT,"Synthetic","forbidden@local","synthetic-H1"));
  denied(ACCESS_DENIED,()->accounts.setAccountActive(admin(),UUID.randomUUID(),false)); assertEquals(5,count("users"));
 }

 @Test void tutorFutureSlotUsesActualClockAcrossSubmicrosecondBoundary() {
  slot(T1,START,END,"AVAILABLE");
  clock.now.set(START.minusNanos(1));
  denied(INVALID_STATE,()->accounts.setAccountActive(admin(),T1,false));
  assertTrue(accounts.findAccountState(T1).orElseThrow().user().isActive());
  clock.now.set(START); assertFalse(accounts.setAccountActive(admin(),T1,false).user().isActive());
  accounts.setAccountActive(admin(),T1,true);
  clock.now.set(START.plusNanos(1)); assertFalse(accounts.setAccountActive(admin(),T1,false).user().isActive());
 }

}
