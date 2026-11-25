package br.com.formentini.eventManager.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventRepository
        extends JpaRepository<Event, Long> {

    List<Event> findByLocationId(Long locationId);
    Page<Event> findByLocationId(Long locationId, Pageable pageable);

    List<Event> findByLocationInstitutionId(Long institutionId);

    @Query("""
            select count(event) > 0 from Event event
            where event.location.id = :locationId
              and event.status in (br.com.formentini.eventManager.event.enums.EventStatus.DRAFT,
                                   br.com.formentini.eventManager.event.enums.EventStatus.PUBLISHED)
              and event.startDate < :endDate
              and event.endDate > :startDate
            """)
    boolean existsByLocationIdAndStartDateLessThanAndEndDateGreaterThan(
            @Param("locationId") Long locationId,
            @Param("endDate") OffsetDateTime endDate,
            @Param("startDate") OffsetDateTime startDate
    );

    @Query("""
            select count(event) > 0 from Event event
            where event.location.id = :locationId
              and event.status in (br.com.formentini.eventManager.event.enums.EventStatus.DRAFT,
                                   br.com.formentini.eventManager.event.enums.EventStatus.PUBLISHED)
              and event.startDate < :endDate
              and event.endDate > :startDate
              and event.id <> :eventId
            """)
    boolean existsByLocationIdAndStartDateLessThanAndEndDateGreaterThanAndIdNot(
            @Param("locationId") Long locationId,
            @Param("endDate") OffsetDateTime endDate,
            @Param("startDate") OffsetDateTime startDate,
            @Param("eventId") Long eventId
    );

}