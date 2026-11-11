package br.com.formentini.eventManager.event.messaging;

import br.com.formentini.eventManager.event.enums.EventStatus;

import java.time.OffsetDateTime;

public record EventCreated(
        Long eventId,
        String title,
        String description,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        EventStatus status,
        Long locationId,
        String locationName
) {
}