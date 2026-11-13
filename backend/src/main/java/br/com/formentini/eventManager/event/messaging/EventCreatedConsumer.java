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

        log.info(
                "Processing EventCreated message. Event ID: {}",
                event.eventId()
        );

        process(event);

        log.info(
                "EventCreated message processed successfully. Event ID: {}",
                event.eventId()
        );
    }

    private void process(EventCreated event) {

        // Aqui ficará a lógica real de processamento.
        // Exemplo: envio de notificações ou integração com outro serviço.

        log.info(
                "Event ID: {} | Title: {} | Location: {}",
                event.eventId(),
                event.title(),
                event.locationName()
        );
    }
}