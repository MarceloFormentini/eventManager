package br.com.formentini.eventManager.event.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import br.com.formentini.eventManager.outbox.OutboxMetrics;

@Component
public class EventCreatedConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(EventCreatedConsumer.class);

    private final EventCreatedHandler handler;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final OutboxMetrics metrics;

    public EventCreatedConsumer(
            EventCreatedHandler handler,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            OutboxMetrics metrics
    ) {
        this.handler = handler;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.metrics = metrics;
    }

    @RabbitListener(queues = "event.created.queue")
    @Transactional
    public void consume(
            EventCreated event,
            @Header(name = AmqpHeaders.CORRELATION_ID, required = false)
            String correlationId
    ) {
        String messageCorrelationId = correlationId == null
                ? Long.toString(event.eventId())
                : correlationId;

        try (MDC.MDCCloseable ignored =
                     MDC.putCloseable("correlationId", messageCorrelationId)) {
            consumeCorrelated(event);
        }
    }

    private void consumeCorrelated(EventCreated event) {
        log.info("Received EventCreated message. Event ID: {}", event.eventId());
        try {
            JdbcTemplate jdbcTemplate =
                    jdbcTemplateProvider.getIfAvailable();

            if (jdbcTemplate != null) {
                int inserted = jdbcTemplate.update("""
                        insert into processed_event (event_id)
                        values (?)
                        on conflict (event_id) do nothing
                        """, event.eventId());

                if (inserted == 0) {
                    log.info(
                            "Duplicate EventCreated ignored. Event ID: {}",
                            event.eventId()
                    );
                    metrics.recordConsumerDuplicate();
                    return;
                }
            }

            handler.handle(event);
            metrics.recordConsumerProcessed();
            log.info(
                    "EventCreated message processed successfully. Event ID: {}",
                    event.eventId()
            );
        } catch (RuntimeException exception) {
            metrics.recordConsumerFailure();
            throw exception;
        }
    }
}