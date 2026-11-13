
package br.com.formentini.eventManager.outbox;

import br.com.formentini.eventManager.outbox.enums.OutboxStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OutboxServiceIntegrationTest {

    private final OutboxService outboxService;
    private final OutboxEventRepository outboxEventRepository;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    OutboxServiceIntegrationTest(
            OutboxService outboxService,
            OutboxEventRepository outboxEventRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.outboxService = outboxService;
        this.outboxEventRepository = outboxEventRepository;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    @Test
    void shouldPersistOutboxEventInDatabase() {

        // Arrange
        Long aggregateId = Math.abs(UUID.randomUUID().getMostSignificantBits());

        TestEvent event = new TestEvent(
                aggregateId,
                "Evento de integração"
        );

        // Act
        transactionTemplate.executeWithoutResult(status ->
                outboxService.save(
                        "EVENT",
                        aggregateId,
                        "EventCreated",
                        event
                )
        );

        // Assert
        OutboxEvent saved = outboxEventRepository.findAll()
                .stream()
                .filter(item ->
                        item.getAggregateId().equals(aggregateId)
                )
                .findFirst()
                .orElseThrow();

        assertNotNull(saved.getId());
        assertEquals("EVENT", saved.getAggregateType());
        assertEquals(aggregateId, saved.getAggregateId());
        assertEquals("EventCreated", saved.getEventType());

        assertEquals(OutboxStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getAttempts());

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getNextAttemptAt());
        assertNull(saved.getPublishedAt());

        assertTrue(
                saved.getPayload().contains("Evento de integração")
        );

        // Cleanup
        outboxEventRepository.deleteById(saved.getId());
    }

    @Test
    void shouldRequireAnExistingTransaction() {

        // Arrange
        Long aggregateId = Math.abs(UUID.randomUUID().getMostSignificantBits());

        TestEvent event = new TestEvent(
                aggregateId,
                "Evento sem transação"
        );

        // Act and Assert
        assertThrows(
                IllegalTransactionStateException.class,
                () -> outboxService.save(
                        "EVENT",
                        aggregateId,
                        "EventCreated",
                        event
                )
        );

        assertTrue(
                outboxEventRepository.findAll()
                        .stream()
                        .noneMatch(item ->
                                item.getAggregateId().equals(aggregateId)
                        )
        );
    }

    private record TestEvent(
            Long eventId,
            String title
    ) {
    }
}
