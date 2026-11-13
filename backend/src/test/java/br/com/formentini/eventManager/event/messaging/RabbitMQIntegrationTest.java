
package br.com.formentini.eventManager.event.messaging;

import br.com.formentini.eventManager.config.RabbitMQConfig;
import br.com.formentini.eventManager.event.enums.EventStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = RabbitMQIntegrationTest.TestApplication.class
)
@Testcontainers
class RabbitMQIntegrationTest {

    @Container
    static final RabbitMQContainer rabbitMQ =
            new RabbitMQContainer("rabbitmq:4-management");

    @DynamicPropertySource
    static void configureRabbitMQ(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;

    @Autowired
    RabbitMQIntegrationTest(
        RabbitTemplate rabbitTemplate,
        RabbitAdmin rabbitAdmin
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
    }

    @Test
    void shouldPublishAndReceiveEventCreatedMessage() {

        EventCreated event = new EventCreated(
                1L,
                "Evento de integração",
                "Teste de integração com RabbitMQ",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(2),
                EventStatus.PUBLISHED,
                10L,
                "Auditório principal"
        );

        // Garante que a infraestrutura esteja declarada.
        rabbitAdmin.initialize();

        // Publica a mensagem.
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_CREATED_ROUTING_KEY,
                event
        );

        // Aguarda a mensagem na fila.
        Object received = rabbitTemplate.receiveAndConvert(
                RabbitMQConfig.EVENT_CREATED_QUEUE,
                5000
        );

        assertNotNull(received);
        assertInstanceOf(EventCreated.class, received);

        EventCreated receivedEvent = (EventCreated) received;

        assertEquals(event.eventId(), receivedEvent.eventId());
        assertEquals(event.title(), receivedEvent.title());
        assertEquals(event.description(), receivedEvent.description());
        assertEquals(event.status(), receivedEvent.status());
        assertEquals(event.locationId(), receivedEvent.locationId());
        assertEquals(event.locationName(), receivedEvent.locationName());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(RabbitMQConfig.class)
    static class TestApplication {
    }
}
