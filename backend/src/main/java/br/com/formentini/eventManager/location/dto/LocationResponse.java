
package br.com.formentini.eventManager.location.dto;

import java.time.OffsetDateTime;

public record LocationResponse(
        Long id,
        String name,
        Long institutionId,
        String institutionName,
        OffsetDateTime createdAt
) {
}
