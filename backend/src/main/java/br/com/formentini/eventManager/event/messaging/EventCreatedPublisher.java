
package br.com.formentini.eventManager.event.messaging;

import br.com.formentini.eventManager.config.RabbitMQConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class EventCreatedPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(EventCreatedPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EventCreatedPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;

        configureConfirmCallback();
        configureReturnsCallback();
    }

    private void configureConfirmCallback() {

        rabbitTemplate.setConfirmCallback(
                (correlationData, ack, cause) -> {

                    if (correlationData == null) {
                        log.warn(
                                "Publisher confirm received without correlation data"
                        );
                        return;
                    }

                    if (ack) {
                        if (correlationData.getReturned() != null) {
                            log.warn(
                                    "Message confirmed by broker but returned as unroutable. Correlation ID: {}",
                                    correlationData.getId()
                            );
                            return;
                        }

                        log.info(
                                "Message successfully confirmed by RabbitMQ. Correlation ID: {}",
                                correlationData.getId()
                        );

                    } else {
                        log.error(
                                "RabbitMQ rejected message. Correlation ID: {}, cause: {}",
                                correlationData.getId(),
                                cause
                        );
                    }
                }
        );
    }

    private void configureReturnsCallback() {

        rabbitTemplate.setReturnsCallback(returned -> {

            log.error(
                    "Message returned by RabbitMQ. Exchange: {}, Routing Key: {}, Reply Code: {}, Reply Text: {}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText()
            );
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(EventCreated event) {

        String correlationId = UUID.randomUUID().toString();

        CorrelationData correlationData =
                new CorrelationData(correlationId);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_CREATED_ROUTING_KEY,
                event,
                correlationData
        );

        log.info(
                "EventCreated message sent to RabbitMQ. Event ID: {}, Correlation ID: {}",
                event.eventId(),
                correlationId
        );
    }
}
