package br.com.formentini.eventManager.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

import br.com.formentini.eventManager.event.enums.EventStatus;

public record EventRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        @Size(max = 1000)
        String description,

        @NotNull
        OffsetDateTime startDate,

        @NotNull
        OffsetDateTime endDate,

        @NotNull
        @Positive
        Long locationId,

        @NotNull
        EventStatus status

) {
}