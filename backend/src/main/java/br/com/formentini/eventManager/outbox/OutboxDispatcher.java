package br.com.formentini.eventManager.outbox;

import br.com.formentini.eventManager.config.RabbitMQConfig;
import br.com.formentini.eventManager.event.messaging.EventCreated;
import br.com.formentini.eventManager.outbox.enums.OutboxStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxMetrics metrics;
    private final int batchSize;
    private final int maxAttempts;
    private final long confirmationTimeoutMs;
    private final long retryInitialDelayMs;
    private final long retryMaxDelayMs;

    public OutboxDispatcher(
            OutboxEventRepository repository,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            OutboxMetrics metrics,
            @Value("${outbox.dispatcher.batch-size:100}") int batchSize,
            @Value("${outbox.dispatcher.max-attempts:5}") int maxAttempts,
            @Value("${outbox.dispatcher.confirmation-timeout-ms:10000}") long confirmationTimeoutMs,
            @Value("${outbox.dispatcher.retry-initial-delay-ms:1000}") long retryInitialDelayMs,
            @Value("${outbox.dispatcher.retry-max-delay-ms:60000}") long retryMaxDelayMs
    ) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.confirmationTimeoutMs = confirmationTimeoutMs;
        this.retryInitialDelayMs = retryInitialDelayMs;
        this.retryMaxDelayMs = retryMaxDelayMs;
    }

    @Scheduled(fixedDelayString = "${outbox.dispatcher.fixed-delay-ms:1000}")
    @Transactional
    public void dispatch() {
        for (OutboxEvent event : repository.findPendingForDispatch(
                OffsetDateTime.now(),
                PageRequest.of(0, batchSize)
        )) {
            dispatchEvent(event.getId());
        }
    }

    @Transactional
    public void dispatchEvent(java.util.UUID id) {
        OutboxEvent event = repository.findById(id).orElse(null);
        if (event == null
                || event.getStatus() != OutboxStatus.PENDING
                || event.getNextAttemptAt().isAfter(OffsetDateTime.now())) {
            return;
        }

        CorrelationData correlationData =
                new CorrelationData(correlationId(event));

        try (MDC.MDCCloseable ignored =
                     MDC.putCloseable("correlationId", correlationId(event))) {
            try {
                Object payload = deserialize(event);
                MessagePostProcessor correlationHeader = message -> {
                    message.getMessageProperties()
                            .setCorrelationId(correlationId(event));
                    return message;
                };
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EVENT_EXCHANGE,
                        RabbitMQConfig.EVENT_CREATED_ROUTING_KEY,
                        payload,
                        correlationHeader,
                        correlationData
                );

                CorrelationData.Confirm confirm =
                        correlationData.getFuture().get(
                                confirmationTimeoutMs,
                                TimeUnit.MILLISECONDS
                        );

                if (!confirm.isAck()) {
                    throw new IllegalStateException(
                            "RabbitMQ rejected message: " + confirm.getReason()
                    );
                }
                if (correlationData.getReturned() != null) {
                    throw new IllegalStateException("Message was returned by RabbitMQ");
                }

                event.markPublished(OffsetDateTime.now());
                repository.save(event);
                metrics.recordPublishSuccess();
                log.info("Outbox event published successfully: {}", event.getId());
            } catch (Exception exception) {
                scheduleRetry(event, exception);
                repository.save(event);
                metrics.recordPublishFailure();
                log.error("Outbox event publication failed: {}", event.getId(), exception);
            }
        }
    }

    private String correlationId(OutboxEvent event) {
        return event.getCorrelationId() == null
                ? event.getId().toString()
                : event.getCorrelationId();
    }

    private Object deserialize(OutboxEvent event) throws Exception {
        if ("EventCreated".equals(event.getEventType())) {
            return objectMapper.readValue(event.getPayload(), EventCreated.class);
        }
        throw new IllegalArgumentException(
                "Unsupported outbox event type: " + event.getEventType()
        );
    }

    private void scheduleRetry(OutboxEvent event, Exception exception) {
        long delay = Math.min(
                retryInitialDelayMs * (1L << Math.min(event.getAttempts(), 20)),
                retryMaxDelayMs
        );
        String error = exception.getMessage();
        if (error == null || error.isBlank()) {
            error = exception.getClass().getSimpleName();
        }
        event.markFailed(
                error,
                OffsetDateTime.now().plusNanos(TimeUnit.MILLISECONDS.toNanos(delay)),
                maxAttempts
        );
    }
}
