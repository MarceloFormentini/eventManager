package br.com.formentini.eventManager.event.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EventCreatedHandler {

    private static final Logger log =
            LoggerFactory.getLogger(EventCreatedHandler.class);

    public void handle(EventCreated event) {

        log.info(
                "Processing event. ID: {}, title: {}, location: {}",
                event.eventId(),
                event.title(),
                event.locationName()
        );

        // Futuras regras de processamento:
        // - Enviar e-mail;
        // - Solicitar notificações;
        // - Integrar com outros serviços;
        // - Registrar histórico de processamento.
    }
}