package br.com.formentini.eventManager.event.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EventCreatedConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(EventCreatedConsumer.class);

    private final EventCreatedHandler handler;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public EventCreatedConsumer(
            EventCreatedHandler handler,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider
    ) {
        this.handler = handler;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    @RabbitListener(queues = "event.created.queue")
    @Transactional
    public void consume(EventCreated event) {

        log.info(
                "Received EventCreated message. Event ID: {}",
                event.eventId()
        );

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
                return;
            }
        }

        handler.handle(event);

        log.info(
                "EventCreated message processed successfully. Event ID: {}",
                event.eventId()
        );
    }
}