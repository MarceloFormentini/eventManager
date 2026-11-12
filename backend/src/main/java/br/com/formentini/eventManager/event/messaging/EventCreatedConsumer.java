package br.com.formentini.eventManager.event.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EventCreatedConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(EventCreatedConsumer.class);

    @RabbitListener(queues = "event.created.queue")
    public void consume(EventCreated event) {

        log.info("Event created message received");
        log.info("Event ID: {}", event.eventId());
        log.info("Title: {}", event.title());
        log.info("Location: {}", event.locationName());
        log.info("Status: {}", event.status());
    }
}