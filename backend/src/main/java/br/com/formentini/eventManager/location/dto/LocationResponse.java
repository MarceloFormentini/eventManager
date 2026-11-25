
package br.com.formentini.eventManager.location.dto;

import java.time.OffsetDateTime;

public record LocationResponse(
        Long id,
        String name,
        Long institutionId,
        String institutionName,
        Integer capacity,
        String description,
        OffsetDateTime createdAt
) {
    public LocationResponse(
            Long id,
            String name,
            Long institutionId,
            String institutionName,
            OffsetDateTime createdAt
    ) {
        this(id, name, institutionId, institutionName, 0, null, createdAt);
    }
}
