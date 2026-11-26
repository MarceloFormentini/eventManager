package br.com.formentini.eventManager.event;

import br.com.formentini.eventManager.event.enums.EventStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventStatusTransitionTest {

    @ParameterizedTest
    @CsvSource({
            "DRAFT, PUBLISHED",
            "DRAFT, CANCELLED",
            "PUBLISHED, CANCELLED",
            "PUBLISHED, COMPLETED"
    })
    void shouldAllowDefinedTransitions(String currentValue, String nextValue) {
        EventStatus current = EventStatus.valueOf(currentValue);
        EventStatus next = EventStatus.valueOf(nextValue);
        Event event = eventWithStatus(current);

        event.changeStatus(next);

        assertEquals(next, event.getStatus());
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, COMPLETED",
            "PUBLISHED, DRAFT",
            "CANCELLED, DRAFT",
            "CANCELLED, PUBLISHED",
            "CANCELLED, COMPLETED",
            "COMPLETED, DRAFT",
            "COMPLETED, PUBLISHED",
            "COMPLETED, CANCELLED"
    })
    void shouldRejectUndefinedTransitions(String currentValue, String nextValue) {
        Event event = eventWithStatus(EventStatus.valueOf(currentValue));

        assertThrows(
                IllegalArgumentException.class,
                () -> event.changeStatus(EventStatus.valueOf(nextValue))
        );
    }

    @Test
    void shouldAllowRepeatingCurrentStatus() {
        for (EventStatus status : EventStatus.values()) {
            Event event = eventWithStatus(status);

            assertDoesNotThrow(() -> event.changeStatus(status));
            assertEquals(status, event.getStatus());
        }
    }

    private Event eventWithStatus(EventStatus status) {
        return new Event(
                null,
                "Evento de teste",
                null,
                null,
                null,
                status
        );
    }
}
