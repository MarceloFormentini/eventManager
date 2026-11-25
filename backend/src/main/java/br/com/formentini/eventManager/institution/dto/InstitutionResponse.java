
package br.com.formentini.eventManager.institution.dto;

import java.time.OffsetDateTime;

public record InstitutionResponse(
    Long id,
    String name,
    Long typeInstitutionId,
    String typeInstitutionName,
    String address,
    String phone,
    String email,
    OffsetDateTime createdAt
) {
    public InstitutionResponse(
            Long id,
            String name,
            Long typeInstitutionId,
            String typeInstitutionName,
            OffsetDateTime createdAt
    ) {
        this(id, name, typeInstitutionId, typeInstitutionName, null, null, null, createdAt);
    }
}
