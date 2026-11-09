
package br.com.formentini.eventManager.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LocationRequest(

        @NotBlank(message = "O nome do local é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String name,

        @NotNull(message = "A instituição é obrigatória")
        @Positive(message = "O ID da instituição deve ser positivo")
        Long institutionId

) {
}
