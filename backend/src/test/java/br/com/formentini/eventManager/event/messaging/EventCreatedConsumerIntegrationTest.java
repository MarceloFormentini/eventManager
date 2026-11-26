
package br.com.formentini.eventManager.event.messaging;

import br.com.formentini.eventManager.config.RabbitMQConfig;
import br.com.formentini.eventManager.outbox.OutboxMetrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.slf4j.MDC;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

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

    private static final AtomicLong eventIds = new AtomicLong(100L);

    @Container
    static final RabbitMQContainer rabbitMQ =
            new RabbitMQContainer("rabbitmq:4-management");

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("event_manager_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;

    @MockitoBean
    private EventCreatedHandler handler;

    @MockitoBean
    private OutboxMetrics outboxMetrics;

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
        verify(outboxMetrics).recordConsumerProcessed();

        assertEquals(
                0,
                rabbitAdmin.getQueueInfo(
                        RabbitMQConfig.EVENT_CREATED_DLQ
                ).getMessageCount()
        );
    }

    @Test
    void shouldIgnoreDuplicateDeliveriesAfterTheFirstSuccessfulProcessing() {
        EventCreated event = createEvent();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_CREATED_ROUTING_KEY,
                event
        );
        verify(handler, timeout(5000).times(1)).handle(any(EventCreated.class));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_CREATED_ROUTING_KEY,
                event
        );

        awaitQueueToDrain(RabbitMQConfig.EVENT_CREATED_QUEUE);
        verify(outboxMetrics, timeout(5000)).recordConsumerDuplicate();
        verify(handler, times(1)).handle(any(EventCreated.class));
        assertEquals(
                0,
                rabbitAdmin.getQueueInfo(RabbitMQConfig.EVENT_CREATED_DLQ)
                        .getMessageCount()
        );
    }

    @Test
    void shouldExposePublisherCorrelationIdWhileHandlingMessage() {
        EventCreated event = createEvent();
        String correlationId = "request-correlation-01";
        AtomicReference<String> observedCorrelationId = new AtomicReference<>();
        doAnswer(invocation -> {
            observedCorrelationId.set(MDC.get("correlationId"));
            return null;
        }).when(handler).handle(any(EventCreated.class));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_CREATED_ROUTING_KEY,
                event,
                message -> {
                    message.getMessageProperties().setCorrelationId(correlationId);
                    return message;
                }
        );

        verify(handler, timeout(5000).times(1)).handle(any(EventCreated.class));
        assertEquals(correlationId, observedCorrelationId.get());
    }

    @Test
    void shouldUseEventIdAsCorrelationIdWhenMessageHeaderIsMissing() {
        EventCreated event = createEvent();
        AtomicReference<String> observedCorrelationId = new AtomicReference<>();
        doAnswer(invocation -> {
            observedCorrelationId.set(MDC.get("correlationId"));
            return null;
        }).when(handler).handle(any(EventCreated.class));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_CREATED_ROUTING_KEY,
                event
        );

        verify(handler, timeout(5000).times(1)).handle(any(EventCreated.class));
        assertEquals(Long.toString(event.eventId()), observedCorrelationId.get());
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
        verify(outboxMetrics, times(2)).recordConsumerFailure();
        verify(outboxMetrics).recordConsumerProcessed();

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
        verify(outboxMetrics, times(3)).recordConsumerFailure();

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

    @Test
    void shouldRouteMalformedJsonMessageToDeadLetterQueue() {
        byte[] invalidJson = "{ invalid json".getBytes(StandardCharsets.UTF_8);
        Message message = MessageBuilder.withBody(invalidJson)
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();

        rabbitTemplate.send(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_CREATED_ROUTING_KEY,
                message
        );

        Message deadLetter = rabbitTemplate.receive(
                RabbitMQConfig.EVENT_CREATED_DLQ,
                10000
        );

        assertNotNull(deadLetter);
        assertArrayEquals(invalidJson, deadLetter.getBody());
        assertTrue(deadLetter.getMessageProperties().getHeaders()
                .containsKey("x-death"));
        verifyNoInteractions(handler);
    }

    private void awaitQueueToDrain(String queueName) {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            var queueInfo = rabbitAdmin.getQueueInfo(queueName);
            if (queueInfo != null && queueInfo.getMessageCount() == 0) {
                return;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while awaiting queue drain", exception);
            }
        }
        fail("Queue did not drain before timeout: " + queueName);
    }

    private EventCreated createEvent() {
        return new EventCreated(
                eventIds.getAndIncrement(),
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
