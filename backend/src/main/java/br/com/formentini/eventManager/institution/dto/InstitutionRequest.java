
package br.com.formentini.eventManager.institution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Email;

public record InstitutionRequest(

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    String name,

    @NotNull(message = "O tipo de instituição é obrigatório")
    @Positive(message = "O ID do tipo de instituição deve ser positivo")
    Long typeInstitutionId,

    @Size(max = 300, message = "O endereço deve ter no máximo 300 caracteres")
    String address,

    @Size(max = 50, message = "O telefone deve ter no máximo 50 caracteres")
    String phone,

    @Email(message = "O e-mail deve ser válido")
    @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
    String email
) {
    public InstitutionRequest(String name, Long typeInstitutionId) {
        this(name, typeInstitutionId, null, null, null);
    }

}
