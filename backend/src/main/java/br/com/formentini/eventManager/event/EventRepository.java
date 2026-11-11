package br.com.formentini.eventManager.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface EventRepository
        extends JpaRepository<Event, Long> {

    List<Event> findByLocationId(Long locationId);

    List<Event> findByLocationInstitutionId(Long institutionId);

    boolean existsByLocationIdAndStartDateLessThanAndEndDateGreaterThan(
            Long locationId,
            OffsetDateTime endDate,
            OffsetDateTime startDate
    );

    boolean existsByLocationIdAndStartDateLessThanAndEndDateGreaterThanAndIdNot(
            Long locationId,
            OffsetDateTime endDate,
            OffsetDateTime startDate,
            Long eventId
    );
}