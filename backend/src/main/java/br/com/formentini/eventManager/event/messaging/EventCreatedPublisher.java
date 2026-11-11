package br.com.formentini.eventManager.event.messaging;

import br.com.formentini.eventManager.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class EventCreatedPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(EventCreatedPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EventCreatedPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(EventCreated event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_CREATED_ROUTING_KEY,
                event
        );

        log.info(
                "Event created message published. Event ID: {}",
                event.eventId()
        );
    }
}