package unichope.platform.core.postgres;

import static org.junit.jupiter.api.Assertions.*;
import static unichope.platform.core.LifecycleFailure.Code.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import unichope.platform.core.model.consultation.BookingStatus;
import unichope.platform.core.model.consultation.SlotStatus;

@Tag("postgres")
class PostgresLifecycleTest extends LifecyclePgFixture {
    @Test
    void td01RequiredArgumentsAndNullTimesHaveTypedFailuresWithoutWrites() {
        failure(INVALID_INPUT,()->lifecycle.createSlot(null,M,START,END));
        failure(INVALID_INPUT,()->lifecycle.createSlot(T1,null,START,END));
        failure(INVALID_TIME,()->lifecycle.createSlot(T1,M,null,END));
        failure(INVALID_TIME,()->lifecycle.createSlot(T1,M,START,null));
        UUID target=slot(T1,START,END,"BOOKED"); UUID booking=booking(S1,target,"ACTIVE");
        for(int op=0;op<4;op++) {
            int operation=op;
            failure(INVALID_INPUT,()->invoke(operation,null,target,booking));
            failure(INVALID_INPUT,()->invoke(operation,operation==1||operation==2?S1:T1,null,null));
        }
    }

    @Test
    void td02InvalidActorRejectedForEveryOperationAndBothPresentMissingResources() {
        UUID target=slot(T1,START,END,"BOOKED"), booking=booking(S1,target,"ACTIVE");
        db.sql("UPDATE users SET active=false WHERE id IN (:ids)").param("ids",List.of(T2,S2)).update();
        for(UUID actor:List.of(id(999),T2,S2,ADMIN)) for(boolean present:List.of(true,false)) {
            UUID s=present?target:id(888), b=present?booking:id(777);
            failure(ACCESS_DENIED,()->lifecycle.createSlot(actor,present?M:id(666),START,END));
            for(int op=0;op<4;op++) { int operation=op; failure(ACCESS_DENIED,()->invoke(operation,actor,s,b)); }
        }
        failure(ACCESS_DENIED,()->lifecycle.createSlot(S1,M,START,END));
        failure(ACCESS_DENIED,()->lifecycle.bookAvailableSlot(T1,target));
        failure(ACCESS_DENIED,()->lifecycle.cancelAvailableSlot(S1,target));
        failure(ACCESS_DENIED,()->lifecycle.cancelActiveBooking(T1,booking));
        failure(ACCESS_DENIED,()->lifecycle.completeActiveBooking(S1,booking));
    }

    @Test
    void td03CreatePersistsExactGeneratedSlotAndTd28SurvivesReconnection() {
        var created=lifecycle.createSlot(T1,M,START,END);
        assertNotNull(created.id()); assertEquals(T1,created.tutorId()); assertEquals(M,created.moduleId());
        assertEquals(START,created.startTime()); assertEquals(END,created.endTime()); assertEquals(SlotStatus.AVAILABLE,created.status());
        assertEquals(created,new PostgresSlotRepository(db).findById(created.id()).orElseThrow());
        assertEquals(1,count("slots")); assertEquals(0,count("bookings"));
    }

    @Test
    void td04StrictTimeAndPositiveDurationWithSupportedPrecisionBoundary() {
        for(Instant start:List.of(N,N.minusSeconds(1))) failure(INVALID_TIME,()->lifecycle.createSlot(T1,M,start,END));
        for(Instant end:List.of(START,START.minusSeconds(1))) failure(INVALID_TIME,()->lifecycle.createSlot(T1,M,START,end));
        assertEquals(N.plusNanos(1000),lifecycle.createSlot(T1,M,N.plusNanos(1000),END).startTime());
    }

    @Test
    void td05ModuleEligibilityFailuresAreIndistinguishable() {
        String absent=failure(MODULE_UNAVAILABLE,()->lifecycle.createSlot(T1,id(999),START,END)).getMessage();
        db.sql("UPDATE modules SET active=false WHERE id=:id").param("id",M).update();
        assertEquals(absent,failure(MODULE_UNAVAILABLE,()->lifecycle.createSlot(T1,M,START,END)).getMessage());
        db.sql("UPDATE modules SET active=true WHERE id=:id").param("id",M).update();
        db.sql("DELETE FROM tutor_modules WHERE tutor_id=:t AND module_id=:m").param("t",T1).param("m",M).update();
        assertEquals(absent,failure(MODULE_UNAVAILABLE,()->lifecycle.createSlot(T1,M,START,END)).getMessage());
    }

    @Test
    void td06OverlapAcrossModulesHalfOpenAdjacencyAndOtherTutorControl() {
        for(String status:List.of("AVAILABLE","BOOKED")) for(long[] interval:List.of(new long[]{3600,7200},new long[]{4000,6000},new long[]{3000,8000},new long[]{3500,4000},new long[]{7000,8000})) {
            clearConsultations(); slot(T1,M2,START,END,status);
            failure(SLOT_OVERLAP,()->lifecycle.createSlot(T1,M,N.plusSeconds(interval[0]),N.plusSeconds(interval[1])));
        }
        clearConsultations(); slot(T1,START,END,"AVAILABLE");
        lifecycle.createSlot(T1,M,N.plusSeconds(1800),START);
        lifecycle.createSlot(T1,M,END,END.plusSeconds(3600));
        slot(T2,END.plusSeconds(4000),END.plusSeconds(5000),"AVAILABLE");
        lifecycle.createSlot(T1,M,END.plusSeconds(4000),END.plusSeconds(5000));
        assertEquals(5,count("slots"));
    }

    @Test
    void td07CancelledAndCompletedHistoryDoNotBlockNewSlots() {
        slot(T1,START,END,"CANCELLED"); slot(T1,START,END,"COMPLETED");
        lifecycle.createSlot(T1,M,START,END);
        assertEquals(3,count("slots"));
    }

    @Test
    void td08CancelOnlyOwnedAvailableAndPreservePastAndFutureHistory() {
        for(Instant start:List.of(START,N.minusSeconds(3600))) {
            clearConsultations(); UUID target=slot(T1,start,start.plusSeconds(1800),"AVAILABLE");
            assertEquals(SlotStatus.CANCELLED,lifecycle.cancelAvailableSlot(T1,target).status());
            assertEquals("CANCELLED",slotStatus(target)); assertEquals(1,count("slots"));
            failure(INVALID_STATE,()->lifecycle.cancelAvailableSlot(T1,target));
        }
        for(String state:List.of("BOOKED","COMPLETED","CANCELLED")) {
            clearConsultations(); UUID target=slot(T1,START,END,state);
            failure(INVALID_STATE,()->lifecycle.cancelAvailableSlot(T1,target));
        }
    }

    @Test
    void td09ForeignAndAbsentOwnedResourcesHaveIdenticalNotFound() {
        UUID target=slot(T2,START,END,"AVAILABLE");
        assertEquals(failure(NOT_FOUND,()->lifecycle.cancelAvailableSlot(T1,target)).getMessage(),
                failure(NOT_FOUND,()->lifecycle.cancelAvailableSlot(T1,id(999))).getMessage());
        UUID booked=slot(T2,START,END,"BOOKED"), booking=booking(S2,booked,"ACTIVE");
        assertEquals(failure(NOT_FOUND,()->lifecycle.cancelActiveBooking(S1,booking)).getMessage(),
                failure(NOT_FOUND,()->lifecycle.cancelActiveBooking(S1,id(999))).getMessage());
        assertEquals(failure(NOT_FOUND,()->lifecycle.completeActiveBooking(T1,booking)).getMessage(),
                failure(NOT_FOUND,()->lifecycle.completeActiveBooking(T1,id(999))).getMessage());
    }

    @Test
    void td10BookAtomicallyAndRejectRelatedEligibilityWithoutDisclosure() {
        UUID target=slot(T1,START,END,"AVAILABLE"); var result=lifecycle.bookAvailableSlot(S1,target);
        assertEquals(S1,result.studentId()); assertEquals(target,result.slotId()); assertEquals(N,result.createdAt());
        assertEquals(BookingStatus.ACTIVE,result.status()); assertEquals("BOOKED",slotStatus(target));
        assertEquals(result,new PostgresBookingRepository(db).findById(result.id()).orElseThrow());
        String message=null;
        for(String change:List.of("UPDATE users SET active=false WHERE id='"+T1+"'","UPDATE modules SET active=false WHERE id='"+M+"'","DELETE FROM tutor_modules WHERE tutor_id='"+T1+"' AND module_id='"+M+"'")) {
            clearConsultations(); db.sql("UPDATE users SET active=true").update(); db.sql("UPDATE modules SET active=true").update();
            UUID next=slot(T1,START,END,"AVAILABLE"); db.sql(change).update();
            String current=failure(MODULE_UNAVAILABLE,()->lifecycle.bookAvailableSlot(S1,next)).getMessage();
            if(message!=null) assertEquals(message,current); message=current;
        }
    }

    @Test
    void td11BookingRejectsInvalidStatesNonfutureAndExistingActiveConflict() {
        for(String state:List.of("BOOKED","CANCELLED","COMPLETED")) {
            clearConsultations(); UUID target=slot(T1,START,END,state); failure(INVALID_STATE,()->lifecycle.bookAvailableSlot(S1,target));
        }
        for(Instant start:List.of(N,N.minusSeconds(1))) {
            clearConsultations(); UUID target=slot(T1,start,END,"AVAILABLE"); failure(INVALID_STATE,()->lifecycle.bookAvailableSlot(S1,target));
        }
        clearConsultations(); UUID target=slot(T1,START,END,"AVAILABLE"); booking(S2,target,"ACTIVE");
        failure(INVALID_STATE,()->lifecycle.bookAvailableSlot(S1,target));
    }

    @Test
    void td12StudentActiveOverlapButNotAdjacencyHistoryOrAnotherStudent() {
        for(String status:List.of("ACTIVE","CANCELLED","COMPLETED")) for(boolean sameStudent:List.of(true,false)) {
            clearConsultations(); UUID prior=slot(T2,START,END,"BOOKED"); booking(sameStudent?S1:S2,prior,status);
            UUID target=slot(T1,START.plusSeconds(1800),END.plusSeconds(1800),"AVAILABLE");
            if(status.equals("ACTIVE")&&sameStudent) failure(BOOKING_OVERLAP,()->lifecycle.bookAvailableSlot(S1,target));
            else assertEquals(BookingStatus.ACTIVE,lifecycle.bookAvailableSlot(S1,target).status());
        }
        clearConsultations(); UUID prior=slot(T2,START,END,"BOOKED"); booking(S1,prior,"ACTIVE");
        UUID adjacent=slot(T1,END,END.plusSeconds(1800),"AVAILABLE"); lifecycle.bookAvailableSlot(S1,adjacent);
    }

    @Test
    void td13CancellationRetainsHistoryReleasesSlotAndAllowsRebooking() {
        UUID target=slot(T1,START,END,"AVAILABLE"); var original=lifecycle.bookAvailableSlot(S1,target);
        var cancelled=lifecycle.cancelActiveBooking(S1,original.id());
        assertEquals(original.id(),cancelled.id()); assertEquals(BookingStatus.CANCELLED,cancelled.status());
        assertEquals("AVAILABLE",slotStatus(target)); lifecycle.bookAvailableSlot(S2,target);
        assertEquals("CANCELLED",bookingStatus(original.id())); assertEquals("BOOKED",slotStatus(target)); assertEquals(2,count("bookings"));
    }

    @Test
    void td14CancelRejectsInvalidPairsStatesAndStrictTimeBoundary() {
        for(String state:List.of("CANCELLED","COMPLETED")) {
            clearConsultations(); UUID target=slot(T1,START,END,"BOOKED"), b=booking(S1,target,state);
            failure(INVALID_STATE,()->lifecycle.cancelActiveBooking(S1,b));
        }
        for(String state:List.of("AVAILABLE","CANCELLED","COMPLETED")) {
            clearConsultations(); UUID target=slot(T1,START,END,state), b=booking(S1,target,"ACTIVE");
            failure(INVALID_STATE,()->lifecycle.cancelActiveBooking(S1,b));
        }
        for(Instant time:List.of(N,N.minusSeconds(1))) {
            clearConsultations(); UUID target=slot(T1,time,END,"BOOKED"), b=booking(S1,target,"ACTIVE");
            failure(INVALID_STATE,()->lifecycle.cancelActiveBooking(S1,b));
        }
        clearConsultations(); UUID target=slot(T1,N.plusNanos(1000),END,"BOOKED"), b=booking(S1,target,"ACTIVE");
        lifecycle.cancelActiveBooking(S1,b);
    }

    @Test
    void td15CompletionAllowsEarlyLateAndNonactorIneligibility() {
        for(boolean early:List.of(true,false)) {
            clearConsultations(); UUID target=slot(T1,START,END,"BOOKED"), b=booking(S1,target,"ACTIVE");
            if(!early) clock.now.set(END.plusSeconds(1));
            db.sql("UPDATE users SET active=false WHERE id=:id").param("id",S1).update();
            db.sql("UPDATE modules SET active=false WHERE id=:id").param("id",M).update();
            db.sql("DELETE FROM tutor_modules WHERE tutor_id=:id").param("id",T1).update();
            assertEquals(BookingStatus.COMPLETED,lifecycle.completeActiveBooking(T1,b).status());
            assertEquals("COMPLETED",slotStatus(target)); assertEquals(1,count("bookings"));
            failure(INVALID_STATE,()->lifecycle.completeActiveBooking(T1,b));
        }
    }

    @Test
    void td16CompletionRejectsInvalidPairsAndHistoryWithoutWrites() {
        for(String state:List.of("CANCELLED","COMPLETED")) {
            clearConsultations(); UUID target=slot(T1,START,END,"BOOKED"), b=booking(S1,target,state);
            failure(INVALID_STATE,()->lifecycle.completeActiveBooking(T1,b));
        }
        for(String state:List.of("AVAILABLE","CANCELLED","COMPLETED")) {
            clearConsultations(); UUID target=slot(T1,START,END,state), b=booking(S1,target,"ACTIVE");
            failure(INVALID_STATE,()->lifecycle.completeActiveBooking(T1,b));
        }
    }

    @Test
    void r1SlotNanosecondsAreTruncatedBeforeValidationAndMatchPersistedValues() {
        Instant start=START.plusNanos(123456789), end=END.plusNanos(987654321);
        var created=lifecycle.createSlot(T1,M,start,end);
        assertEquals(START.plusNanos(123456000),created.startTime());
        assertEquals(END.plusNanos(987654000),created.endTime());
        assertEquals(created,new PostgresSlotRepository(db).findById(created.id()).orElseThrow());
        lifecycle.createSlot(T1,M,created.endTime(),created.endTime().plusSeconds(1800));
        assertEquals(2,count("slots"));
    }

    @Test
    void r1SubmicroFutureAndCollapsedIntervalsAreTypedInvalidTimeWithoutWrites() {
        failure(INVALID_TIME,()->lifecycle.createSlot(T1,M,N.plusNanos(1),N.plusSeconds(1)));
        failure(INVALID_TIME,()->lifecycle.createSlot(T1,M,START.plusNanos(1),START.plusNanos(999)));
        clock.now.set(N.plusNanos(1500));
        failure(INVALID_TIME,()->lifecycle.createSlot(T1,M,N.plusNanos(1999),END));
        assertEquals(N.plusNanos(2000),lifecycle.createSlot(T1,M,N.plusNanos(2999),END).startTime());
    }

    @Test
    void r1BookingCreatedAtWithNanosecondClockMatchesPersistedMicroseconds() {
        clock.now.set(N.plusNanos(123456789));
        UUID target=slot(T1,START,END,"AVAILABLE");
        var created=lifecycle.bookAvailableSlot(S1,target);
        assertEquals(N.plusNanos(123456000),created.createdAt());
        assertEquals(created,new PostgresBookingRepository(db).findById(created.id()).orElseThrow());
    }

    private Object invoke(int operation, UUID actor, UUID slot, UUID booking) {
        return switch(operation) {
            case 0 -> lifecycle.cancelAvailableSlot(actor,slot);
            case 1 -> lifecycle.bookAvailableSlot(actor,slot);
            case 2 -> lifecycle.cancelActiveBooking(actor,booking);
            case 3 -> lifecycle.completeActiveBooking(actor,booking);
            default -> throw new IllegalArgumentException();
        };
    }
}
