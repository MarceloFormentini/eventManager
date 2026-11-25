
package br.com.formentini.eventManager.institutiontype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TypeInstitutionRequest(

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    String name,

    @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
    String description
) {
    public TypeInstitutionRequest(String name) {
        this(name, null);
    }
}
