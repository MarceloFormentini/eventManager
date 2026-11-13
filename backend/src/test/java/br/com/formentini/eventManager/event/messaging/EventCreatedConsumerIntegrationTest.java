
package br.com.formentini.eventManager.event.messaging;

import br.com.formentini.eventManager.config.RabbitMQConfig;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = EventCreatedConsumerIntegrationTest.TestApplication.class,
        properties = {
                "spring.rabbitmq.listener.simple.retry.enabled=true",
                "spring.rabbitmq.listener.simple.retry.max-attempts=3",
                "spring.rabbitmq.listener.simple.retry.initial-interval=100ms",
                "spring.rabbitmq.listener.simple.retry.multiplier=2.0",
                "spring.rabbitmq.listener.simple.retry.max-interval=1000ms",
                "spring.rabbitmq.listener.simple.default-requeue-rejected=false"
        }
)
@Testcontainers
class EventCreatedConsumerIntegrationTest {

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

    @MockitoBean
    private EventCreatedHandler handler;

    @Autowired
    EventCreatedConsumerIntegrationTest(
            RabbitTemplate rabbitTemplate,
            RabbitAdmin rabbitAdmin
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitAdmin = rabbitAdmin;
    }

    @BeforeEach
    void setUp() {
        rabbitAdmin.initialize();

        rabbitAdmin.purgeQueue(
                RabbitMQConfig.EVENT_CREATED_QUEUE,
                true
        );

        rabbitAdmin.purgeQueue(
                RabbitMQConfig.EVENT_CREATED_DLQ,
                true
        );
    }

    @Test
    void shouldProcessMessageSuccessfully() {

        EventCreated event = createEvent();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_CREATED_ROUTING_KEY,
                event
        );

        verify(handler, timeout(5000).times(1)).handle(any(EventCreated.class));

        assertEquals(
                0,
                rabbitAdmin.getQueueInfo(
                        RabbitMQConfig.EVENT_CREATED_DLQ
                ).getMessageCount()
        );
    }

    @Test
    void shouldRetryWhenProcessingFailsTemporarily() {

        EventCreated event = createEvent();

        doThrow(new IllegalStateException("Temporary failure"))
                .doThrow(new IllegalStateException("Temporary failure"))
                .doNothing()
                .when(handler)
                .handle(any(EventCreated.class));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_CREATED_ROUTING_KEY,
                event
        );

        verify(handler, timeout(10000).times(3))
                .handle(any(EventCreated.class));

        assertEquals(
                0,
                rabbitAdmin.getQueueInfo(
                        RabbitMQConfig.EVENT_CREATED_DLQ
                ).getMessageCount()
        );
    }

    @Test
    void shouldSendMessageToDeadLetterQueueAfterMaxRetries() {

        EventCreated event = createEvent();

        doThrow(new IllegalStateException("Permanent failure"))
                .when(handler)
                .handle(any(EventCreated.class));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_CREATED_ROUTING_KEY,
                event
        );

        verify(handler, timeout(10000).times(3))
                .handle(any(EventCreated.class));

        Object received = rabbitTemplate.receiveAndConvert(
                RabbitMQConfig.EVENT_CREATED_DLQ,
                5000
        );

        assertNotNull(received);
        assertInstanceOf(EventCreated.class, received);

        EventCreated failedEvent = (EventCreated) received;

        assertEquals(event.eventId(), failedEvent.eventId());
        assertEquals(event.title(), failedEvent.title());
    }

    private EventCreated createEvent() {
        return new EventCreated(
                100L,
                "Evento de teste",
                "Descrição do evento",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(2),
                br.com.formentini.eventManager.event.enums.EventStatus.PUBLISHED,
                10L,
                "Auditório principal"
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            RabbitMQConfig.class,
            EventCreatedConsumer.class,
            EventCreatedHandler.class
    })
    static class TestApplication {
    }
}
