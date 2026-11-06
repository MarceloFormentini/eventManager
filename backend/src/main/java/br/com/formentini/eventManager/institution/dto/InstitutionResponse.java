
package br.com.formentini.eventManager.institution.dto;

import java.time.OffsetDateTime;

public record InstitutionResponse(
    Long id,
    String name,
    Long typeInstitutionId,
    String typeInstitutionName,
    OffsetDateTime createdAt
) {}
