package br.com.formentini.eventManager.outbox;

import br.com.formentini.eventManager.outbox.enums.OutboxStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutboxServiceTest {

    private OutboxEventRepository repository;
    private OutboxService service;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        objectMapper = new ObjectMapper();

        service = new OutboxService(repository, objectMapper);
    }

    @Test
    void shouldSerializeAndPersistEvent() {
        TestEvent event = new TestEvent(
                100L,
                "Evento de teste"
        );

        service.save(
                "EVENT",
                100L,
                "EventCreated",
                event
        );

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(repository).save(captor.capture());

        OutboxEvent saved = captor.getValue();

        assertNotNull(saved.getId());
        assertEquals("EVENT", saved.getAggregateType());
        assertEquals(100L, saved.getAggregateId());
        assertEquals("EventCreated", saved.getEventType());

        assertEquals(OutboxStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getAttempts());

        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getCreatedAt(), saved.getNextAttemptAt());

        assertEquals(
                "{\"eventId\":100,\"title\":\"Evento de teste\"}",
                saved.getPayload()
        );
    }

    @Test
    void shouldGenerateUniqueIdentifiersForEachMessage() {
        TestEvent event = new TestEvent(100L, "Evento");

        service.save("EVENT", 100L, "EventCreated", event);
        service.save("EVENT", 100L, "EventCreated", event);

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(repository, times(2)).save(captor.capture());

        UUID firstId = captor.getAllValues().get(0).getId();
        UUID secondId = captor.getAllValues().get(1).getId();

        assertNotEquals(firstId, secondId);
    }

    @Test
    void shouldNotPersistWhenSerializationFails() {
        Object invalidEvent = new Object() {
            public String getValue() {
                throw new IllegalStateException("Serialization error");
            }
        };

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.save(
                        "EVENT",
                        100L,
                        "EventCreated",
                        invalidEvent
                )
        );

        assertEquals("Could not serialize outbox event", exception.getMessage());
        assertNotNull(exception.getCause());
        verifyNoInteractions(repository);
    }

    @Test
    void shouldPropagateRepositoryFailure() {
        IllegalStateException repositoryFailure =
                new IllegalStateException("Database unavailable");
        when(repository.save(any(OutboxEvent.class)))
                .thenThrow(repositoryFailure);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.save(
                        "EVENT",
                        100L,
                        "EventCreated",
                        new TestEvent(100L, "Evento")
                )
        );

        assertSame(repositoryFailure, exception);
        verify(repository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldRequireAnExistingTransaction() throws NoSuchMethodException {
        Method saveMethod = OutboxService.class.getMethod(
                "save",
                String.class,
                Long.class,
                String.class,
                Object.class
        );
        Transactional transactional =
                saveMethod.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(Propagation.MANDATORY, transactional.propagation());
    }

    private record TestEvent(
            Long eventId,
            String title
    ) {
    }
}
