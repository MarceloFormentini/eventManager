package br.com.formentini.eventManager.outbox;

import br.com.formentini.eventManager.event.enums.EventStatus;
import br.com.formentini.eventManager.event.messaging.EventCreated;
import br.com.formentini.eventManager.outbox.enums.OutboxStatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "outbox.dispatcher.fixed-delay-ms=600000",
        "outbox.dispatcher.batch-size=100",
        "outbox.dispatcher.max-attempts=3",
        "outbox.dispatcher.confirmation-timeout-ms=100",
        "outbox.dispatcher.retry-initial-delay-ms=200",
        "outbox.dispatcher.retry-max-delay-ms=250",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.dynamic=false"
})
@Testcontainers
class OutboxDispatcherIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17")
                    .withDatabaseName("event_manager_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OutboxEventRepository repository;

    @Autowired
    private OutboxDispatcher dispatcher;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OutboxMetrics metrics;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        repository.deleteAll();
        acknowledgePublishedMessages();
    }

    @AfterEach
    void tearDown() {
        reset(rabbitTemplate);
    }

    @Test
    void shouldMarkOutboxEventPublishedAfterPositiveConfirmation() throws Exception {
        OutboxEvent event = createPendingEvent();

        dispatcher.dispatch();

        OutboxEvent published = repository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PUBLISHED, published.getStatus());
        assertEquals(0, published.getAttempts());
        assertNotNull(published.getPublishedAt());
        assertNull(published.getLastError());
        ArgumentCaptor<MessagePostProcessor> postProcessor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        ArgumentCaptor<CorrelationData> correlationData =
                ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(EventCreated.class),
                postProcessor.capture(),
                correlationData.capture()
        );
        assertEquals(event.getCorrelationId(), correlationData.getValue().getId());
        Message correlatedMessage = postProcessor.getValue().postProcessMessage(
                new Message(new byte[0], new org.springframework.amqp.core.MessageProperties())
        );
        assertEquals(
                event.getCorrelationId(),
                correlatedMessage.getMessageProperties().getCorrelationId()
        );
    }

    @Test
    void shouldRetryWhenMessageIsReturned() throws Exception {
        OutboxEvent event = createPendingEvent();
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(4);
            correlationData.setReturned(new ReturnedMessage(
                    new Message("{}".getBytes(StandardCharsets.UTF_8)),
                    312,
                    "NO_ROUTE",
                    "event.exchange",
                    "event.created.v1"
            ));
            correlationData.getFuture().complete(
                    new CorrelationData.Confirm(true, null)
            );
            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(EventCreated.class),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );

        OffsetDateTime dispatchStartedAt = OffsetDateTime.now();
        dispatcher.dispatch();

        OutboxEvent retried = repository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PENDING, retried.getStatus());
        assertEquals(1, retried.getAttempts());
        assertEquals("Message was returned by RabbitMQ", retried.getLastError());
        assertRetryDelay(retried.getNextAttemptAt(), dispatchStartedAt, 200);
    }

    @Test
    void shouldRetryOnNegativeConfirmationAndApplyCappedExponentialBackoff()
            throws Exception {
        OutboxEvent event = createPendingEvent();
        rejectPublishedMessages();

        OffsetDateTime firstDispatchStartedAt = OffsetDateTime.now();
        dispatcher.dispatch();

        OutboxEvent firstRetry = repository.findById(event.getId()).orElseThrow();
        assertEquals(1, firstRetry.getAttempts());
        assertEquals(OutboxStatus.PENDING, firstRetry.getStatus());
        assertRetryDelay(firstRetry.getNextAttemptAt(), firstDispatchStartedAt, 200);

        makeDue(firstRetry.getId());
        OffsetDateTime secondDispatchStartedAt = OffsetDateTime.now();
        dispatcher.dispatch();

        OutboxEvent secondRetry = repository.findById(event.getId()).orElseThrow();
        assertEquals(2, secondRetry.getAttempts());
        assertEquals(OutboxStatus.PENDING, secondRetry.getStatus());
        assertRetryDelay(secondRetry.getNextAttemptAt(), secondDispatchStartedAt, 250);

        makeDue(secondRetry.getId());
        dispatcher.dispatch();

        OutboxEvent exhausted = repository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.FAILED, exhausted.getStatus());
        assertEquals(3, exhausted.getAttempts());
        assertEquals("RabbitMQ rejected message: test nack", exhausted.getLastError());
    }

    @Test
    void shouldRetryWhenPublisherConfirmationTimesOut() throws Exception {
        OutboxEvent event = createPendingEvent();
        reset(rabbitTemplate);
        doAnswer(invocation -> null).when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(EventCreated.class),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );

        OffsetDateTime dispatchStartedAt = OffsetDateTime.now();
        dispatcher.dispatch();

        OutboxEvent retried = repository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PENDING, retried.getStatus());
        assertEquals(1, retried.getAttempts());
        assertTrue(retried.getLastError().contains("TimeoutException"));
        assertRetryDelay(retried.getNextAttemptAt(), dispatchStartedAt, 200);
    }

    @Test
    void shouldResumePendingEventsWithANewDispatcherInstance() throws Exception {
        OutboxEvent event = createPendingEvent();
        OutboxDispatcher restartedDispatcher = new OutboxDispatcher(
                repository,
                rabbitTemplate,
                objectMapper,
                metrics,
                100,
                3,
                100,
                100,
                250
        );

        transactionTemplate.executeWithoutResult(
                status -> restartedDispatcher.dispatch()
        );

        assertEquals(
                OutboxStatus.PUBLISHED,
                repository.findById(event.getId()).orElseThrow().getStatus()
        );
    }

    @Test
    void shouldDispatchAnEventOnlyOnceWhenTwoInstancesRunConcurrently()
            throws Exception {
        createPendingEvent();
        CountDownLatch firstPublishStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstPublish = new CountDownLatch(1);
        AtomicInteger publishCalls = new AtomicInteger();
        doAnswer(invocation -> {
            publishCalls.incrementAndGet();
            CorrelationData correlationData = invocation.getArgument(4);
            firstPublishStarted.countDown();
            assertTrue(releaseFirstPublish.await(5, TimeUnit.SECONDS));
            correlationData.getFuture().complete(
                    new CorrelationData.Confirm(true, null)
            );
            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(EventCreated.class),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );

        OutboxDispatcher secondDispatcher = new OutboxDispatcher(
                repository,
                rabbitTemplate,
                objectMapper,
                metrics,
                100,
                3,
                100,
                100,
                250
        );

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(dispatcher::dispatch);
            assertTrue(firstPublishStarted.await(5, TimeUnit.SECONDS));
            var second = executor.submit(() ->
                    transactionTemplate.executeWithoutResult(
                            status -> secondDispatcher.dispatch()
                    )
            );

            awaitDatabaseLockWaiter();
            releaseFirstPublish.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            releaseFirstPublish.countDown();
        }

        assertEquals(1, publishCalls.get());
        assertEquals(1, repository.count());
        assertEquals(
                OutboxStatus.PUBLISHED,
                repository.findAll().getFirst().getStatus()
        );
    }

    private OutboxEvent createPendingEvent() throws Exception {
        EventCreated payload = new EventCreated(
                9001L,
                "Reliability test",
                "Outbox dispatcher integration",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1),
                EventStatus.PUBLISHED,
                100L,
                "Test location"
        );
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                "EVENT",
                payload.eventId(),
                "EventCreated",
                objectMapper.writeValueAsString(payload),
                "test-request-correlation"
        );
        return repository.saveAndFlush(event);
    }

    private void acknowledgePublishedMessages() {
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(4);
            correlationData.getFuture().complete(
                    new CorrelationData.Confirm(true, null)
            );
            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(EventCreated.class),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }

    private void rejectPublishedMessages() {
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(4);
            correlationData.getFuture().complete(
                    new CorrelationData.Confirm(false, "test nack")
            );
            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                any(EventCreated.class),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }

    private void makeDue(UUID id) {
        jdbcTemplate.update(
                "update outbox_event set next_attempt_at = now() - interval '1 second' where id = ?",
                id
        );
    }

    private void assertRetryDelay(
            OffsetDateTime nextAttemptAt,
            OffsetDateTime dispatchStartedAt,
            long expectedDelayMs
    ) {
        long actualDelayMs = java.time.Duration.between(
                dispatchStartedAt,
                nextAttemptAt
        ).toMillis();
        assertTrue(
                actualDelayMs >= expectedDelayMs
                        && actualDelayMs <= expectedDelayMs + 500,
                "Expected retry delay near " + expectedDelayMs
                        + "ms but was " + actualDelayMs + "ms"
        );
    }

    private void awaitDatabaseLockWaiter() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Integer waitingQueries = jdbcTemplate.queryForObject("""
                    select count(*)
                    from pg_stat_activity
                    where wait_event_type = 'Lock'
                      and lower(query) like '%outbox_event%'
                    """, Integer.class);
            if (waitingQueries != null && waitingQueries > 0) {
                return;
            }
            Thread.sleep(25);
        }
        fail("The second dispatcher did not wait for the locked outbox row");
    }
}
