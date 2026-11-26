package br.com.formentini.eventManager.outbox;

import br.com.formentini.eventManager.config.RabbitMQConfig;
import br.com.formentini.eventManager.outbox.enums.OutboxStatus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;

@Component
public class OutboxMetrics {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxMetrics.class);

    private final OutboxEventRepository repository;
    private final ObjectProvider<RabbitAdmin> rabbitAdminProvider;
    private final Counter publishSuccess;
    private final Counter publishFailure;
    private final Map<ConsumerOutcome, Counter> consumerCounters =
            new EnumMap<>(ConsumerOutcome.class);

    public OutboxMetrics(
            MeterRegistry meterRegistry,
            OutboxEventRepository repository,
            ObjectProvider<RabbitAdmin> rabbitAdminProvider
    ) {
        this.repository = repository;
        this.rabbitAdminProvider = rabbitAdminProvider;

        Gauge.builder(
                        "event.outbox.pending",
                        repository,
                        value -> value.countByStatus(OutboxStatus.PENDING)
                )
                .description("Number of pending Outbox events")
                .register(meterRegistry);
        Gauge.builder(
                        "event.outbox.failed",
                        repository,
                        value -> value.countByStatus(OutboxStatus.FAILED)
                )
                .description("Number of Outbox events that exhausted retries")
                .register(meterRegistry);
        Gauge.builder(
                        "event.outbox.pending.attempts",
                        repository,
                        OutboxEventRepository::sumPendingAttempts
                )
                .description("Total retry attempts for pending Outbox events")
                .register(meterRegistry);
        Gauge.builder(
                        "event.outbox.oldest.pending.seconds",
                        repository,
                        this::oldestPendingAgeSeconds
                )
                .description("Age in seconds of the oldest pending Outbox event")
                .register(meterRegistry);
        Gauge.builder(
                        "event.rabbitmq.dlq.messages",
                        this.rabbitAdminProvider,
                        this::deadLetterQueueDepth
                )
                .description("Number of messages waiting in the event DLQ")
                .register(meterRegistry);

        publishSuccess = Counter.builder("event.outbox.publish")
                .tag("result", "success")
                .description("Outbox publish outcomes")
                .register(meterRegistry);
        publishFailure = Counter.builder("event.outbox.publish")
                .tag("result", "failure")
                .description("Outbox publish outcomes")
                .register(meterRegistry);
        for (ConsumerOutcome outcome : ConsumerOutcome.values()) {
            consumerCounters.put(
                    outcome,
                    Counter.builder("event.consumer.messages")
                            .tag("result", outcome.tag)
                            .description("Event consumer message outcomes")
                            .register(meterRegistry)
            );
        }
    }

    public void recordPublishSuccess() {
        publishSuccess.increment();
    }

    public void recordPublishFailure() {
        publishFailure.increment();
    }

    public void recordConsumerProcessed() {
        consumerCounters.get(ConsumerOutcome.PROCESSED).increment();
    }

    public void recordConsumerDuplicate() {
        consumerCounters.get(ConsumerOutcome.DUPLICATE).increment();
    }

    public void recordConsumerFailure() {
        consumerCounters.get(ConsumerOutcome.FAILURE).increment();
    }

    private double oldestPendingAgeSeconds(OutboxEventRepository outboxRepository) {
        return outboxRepository.findOldestPendingCreatedAt()
                .map(createdAt -> Math.max(
                        0,
                        OffsetDateTime.now().toEpochSecond()
                                - createdAt.toEpochSecond()
                ))
                .orElse(0L);
    }

    private double deadLetterQueueDepth(ObjectProvider<RabbitAdmin> adminProvider) {
        RabbitAdmin admin = adminProvider.getIfAvailable();
        if (admin == null) {
            log.error("Cannot read RabbitMQ DLQ depth; RabbitAdmin is unavailable");
            return -1;
        }
        try {
            var queueInfo = admin.getQueueInfo(RabbitMQConfig.EVENT_CREATED_DLQ);
            if (queueInfo == null) {
                log.error(
                        "Cannot read RabbitMQ DLQ depth; queue is missing: {}",
                        RabbitMQConfig.EVENT_CREATED_DLQ
                );
                return -1;
            }
            return queueInfo.getMessageCount();
        } catch (AmqpException exception) {
            log.error(
                    "Cannot read RabbitMQ DLQ depth; RabbitMQ request failed",
                    exception
            );
            return -1;
        }
    }

    private enum ConsumerOutcome {
        PROCESSED("processed"),
        DUPLICATE("duplicate"),
        FAILURE("failure");

        private final String tag;

        ConsumerOutcome(String tag) {
            this.tag = tag;
        }
    }
}
