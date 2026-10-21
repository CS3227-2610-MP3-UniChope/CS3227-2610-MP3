package unichope.platform.core.postgres;

import static org.junit.jupiter.api.Assertions.*;
import static unichope.platform.core.LifecycleFailure.Code.*;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import unichope.platform.core.LifecycleFailure;

class LifecycleInputTest {
    @Test
    void td01InvalidInputFailsWithoutConnectingToAnyDatabase() {
        var lifecycle=new PostgresConsultationLifecycle(new DriverManagerDataSource(),Clock.systemUTC());
        UUID id=UUID.randomUUID(); Instant time=Instant.parse("2026-10-21T00:00:00Z");
        assertEquals(INVALID_INPUT,assertThrows(LifecycleFailure.class,()->lifecycle.createSlot(null,id,time,time)).code());
        assertEquals(INVALID_INPUT,assertThrows(LifecycleFailure.class,()->lifecycle.createSlot(id,null,time,time)).code());
        assertEquals(INVALID_TIME,assertThrows(LifecycleFailure.class,()->lifecycle.createSlot(id,id,null,time)).code());
        assertEquals(INVALID_TIME,assertThrows(LifecycleFailure.class,()->lifecycle.createSlot(id,id,time,null)).code());
        assertEquals(INVALID_INPUT,assertThrows(LifecycleFailure.class,()->lifecycle.cancelAvailableSlot(null,id)).code());
        assertEquals(INVALID_INPUT,assertThrows(LifecycleFailure.class,()->lifecycle.cancelAvailableSlot(id,null)).code());
        assertEquals(INVALID_INPUT,assertThrows(LifecycleFailure.class,()->lifecycle.bookAvailableSlot(null,id)).code());
        assertEquals(INVALID_INPUT,assertThrows(LifecycleFailure.class,()->lifecycle.bookAvailableSlot(id,null)).code());
        assertEquals(INVALID_INPUT,assertThrows(LifecycleFailure.class,()->lifecycle.cancelActiveBooking(null,id)).code());
        assertEquals(INVALID_INPUT,assertThrows(LifecycleFailure.class,()->lifecycle.cancelActiveBooking(id,null)).code());
        assertEquals(INVALID_INPUT,assertThrows(LifecycleFailure.class,()->lifecycle.completeActiveBooking(null,id)).code());
        assertEquals(INVALID_INPUT,assertThrows(LifecycleFailure.class,()->lifecycle.completeActiveBooking(id,null)).code());
    }
}
