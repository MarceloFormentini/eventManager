package br.com.formentini.eventManager.event.messaging;

import br.com.formentini.eventManager.outbox.OutboxMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EventCreatedConsumerMetricsTest {

    @Test
    void shouldCountDatabaseFailuresAsConsumerFailures() {
        EventCreatedHandler handler = mock(EventCreatedHandler.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ObjectProvider<JdbcTemplate> jdbcTemplateProvider =
                mock(ObjectProvider.class);
        OutboxMetrics metrics = mock(OutboxMetrics.class);
        when(jdbcTemplateProvider.getIfAvailable()).thenReturn(jdbcTemplate);
        when(jdbcTemplate.update(anyString(), anyLong()))
                .thenThrow(new DataAccessResourceFailureException(
                        "Database unavailable"
                ));
        EventCreatedConsumer consumer = new EventCreatedConsumer(
                handler,
                jdbcTemplateProvider,
                metrics
        );

        assertThrows(
                DataAccessResourceFailureException.class,
                () -> consumer.consume(sampleEvent(), "request-71")
        );

        verify(metrics).recordConsumerFailure();
        verifyNoInteractions(handler);
    }

    @Test
    void shouldProcessMessageWhenJdbcTemplateIsUnavailable() {
        EventCreatedHandler handler = mock(EventCreatedHandler.class);
        ObjectProvider<JdbcTemplate> jdbcTemplateProvider =
                mock(ObjectProvider.class);
        OutboxMetrics metrics = mock(OutboxMetrics.class);
        when(jdbcTemplateProvider.getIfAvailable()).thenReturn(null);
        EventCreatedConsumer consumer = new EventCreatedConsumer(
                handler,
                jdbcTemplateProvider,
                metrics
        );
        EventCreated event = sampleEvent();

        consumer.consume(event, null);

        verify(handler).handle(event);
        verify(metrics).recordConsumerProcessed();
    }

    private EventCreated sampleEvent() {
        return new EventCreated(
                71L,
                "Event",
                "Description",
                java.time.OffsetDateTime.now(),
                java.time.OffsetDateTime.now().plusHours(1),
                br.com.formentini.eventManager.event.enums.EventStatus.PUBLISHED,
                9L,
                "Location"
        );
    }
}
