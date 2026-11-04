
package br.com.formentini.eventManager.institutiontype.dto;

import java.time.OffsetDateTime;

public record TypeInstitutionResponse(
    Long id,
    String name,
    OffsetDateTime createdAt
) {}
