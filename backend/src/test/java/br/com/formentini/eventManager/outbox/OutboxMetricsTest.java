package br.com.formentini.eventManager.outbox;

import br.com.formentini.eventManager.config.RabbitMQConfig;
import br.com.formentini.eventManager.outbox.enums.OutboxStatus;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutboxMetricsTest {

    @Test
    void shouldExposeOutboxQueueAndConsumerMetrics() {
        var registry = new SimpleMeterRegistry();
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        RabbitAdmin rabbitAdmin = mock(RabbitAdmin.class);
        ObjectProvider<RabbitAdmin> rabbitAdminProvider = mock(ObjectProvider.class);
        when(rabbitAdminProvider.getIfAvailable()).thenReturn(rabbitAdmin);
        when(repository.countByStatus(OutboxStatus.PENDING)).thenReturn(7L);
        when(repository.countByStatus(OutboxStatus.FAILED)).thenReturn(2L);
        when(repository.sumPendingAttempts()).thenReturn(4L);
        when(repository.findOldestPendingCreatedAt())
                .thenReturn(Optional.of(OffsetDateTime.now().minusSeconds(40)));
        when(rabbitAdmin.getQueueInfo(RabbitMQConfig.EVENT_CREATED_DLQ))
                .thenReturn(new QueueInformation(
                        RabbitMQConfig.EVENT_CREATED_DLQ,
                        3,
                        0
                ));
        OutboxMetrics metrics = new OutboxMetrics(
                registry,
                repository,
                rabbitAdminProvider
        );

        metrics.recordPublishSuccess();
        metrics.recordPublishFailure();
        metrics.recordConsumerProcessed();
        metrics.recordConsumerDuplicate();
        metrics.recordConsumerFailure();

        assertEquals(7, registry.get("event.outbox.pending").gauge().value());
        assertEquals(2, registry.get("event.outbox.failed").gauge().value());
        assertEquals(
                4,
                registry.get("event.outbox.pending.attempts").gauge().value()
        );
        assertTrue(registry.get("event.outbox.oldest.pending.seconds")
                .gauge().value() >= 39);
        assertEquals(
                3,
                registry.get("event.rabbitmq.dlq.messages").gauge().value()
        );
        assertEquals(
                1,
                registry.get("event.outbox.publish")
                        .tag("result", "success").counter().count()
        );
        assertEquals(
                1,
                registry.get("event.outbox.publish")
                        .tag("result", "failure").counter().count()
        );
        assertEquals(
                1,
                registry.get("event.consumer.messages")
                        .tag("result", "processed").counter().count()
        );
        assertEquals(
                1,
                registry.get("event.consumer.messages")
                        .tag("result", "duplicate").counter().count()
        );
        assertEquals(
                1,
                registry.get("event.consumer.messages")
                        .tag("result", "failure").counter().count()
        );
    }

    @Test
    void shouldMarkDlqMetricUnavailableWhenQueueIsMissing() {
        var registry = new SimpleMeterRegistry();
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        RabbitAdmin rabbitAdmin = mock(RabbitAdmin.class);
        ObjectProvider<RabbitAdmin> rabbitAdminProvider = mock(ObjectProvider.class);
        when(rabbitAdminProvider.getIfAvailable()).thenReturn(rabbitAdmin);
        when(rabbitAdmin.getQueueInfo(RabbitMQConfig.EVENT_CREATED_DLQ))
                .thenReturn(null);
        new OutboxMetrics(registry, repository, rabbitAdminProvider);

        assertEquals(
                -1,
                registry.get("event.rabbitmq.dlq.messages").gauge().value()
        );
    }
}
