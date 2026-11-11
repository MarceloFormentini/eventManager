package br.com.formentini.eventManager.event.dto;

import java.time.OffsetDateTime;

import br.com.formentini.eventManager.event.enums.EventStatus;

public record EventResponse(
        Long id,
        String title,
        String description,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        EventStatus status,
        Long locationId,
        String locationName,
        OffsetDateTime createdAt
) {
}