package br.com.formentini.eventManager.event.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EventCreatedConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(EventCreatedConsumer.class);

    private final EventCreatedHandler handler;

    public EventCreatedConsumer(EventCreatedHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = "event.created.queue")
    public void consume(EventCreated event) {

        log.info(
                "Received EventCreated message. Event ID: {}",
                event.eventId()
        );

        handler.handle(event);

        log.info(
                "EventCreated message processed successfully. Event ID: {}",
                event.eventId()
        );
    }
}