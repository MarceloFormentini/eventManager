package br.com.formentini.eventManager.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

import br.com.formentini.eventManager.event.enums.EventStatus;

public record EventRequest(

        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        String title,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String description,

        @NotNull(message = "A data e hora de início são obrigatórias")
        OffsetDateTime startDate,

        @NotNull(message = "A data e hora de término são obrigatórias")
        OffsetDateTime endDate,

        @NotNull(message = "O local é obrigatório")
        @Positive(message = "O ID do local deve ser positivo")
        Long locationId,

        @NotNull(message = "O status é obrigatório")
        EventStatus status

) {
}