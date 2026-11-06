
package br.com.formentini.eventManager.institution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public record InstitutionRequest(

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    String name,

    @NotNull(message = "O tipo de instituição é obrigatório")
    @Positive(message = "O ID do tipo de instituição deve ser positivo")
    Long typeInstitutionId

) {}
