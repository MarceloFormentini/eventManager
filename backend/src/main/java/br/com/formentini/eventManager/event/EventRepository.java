package br.com.formentini.eventManager.event;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository
        extends JpaRepository<Event, Long> {

    List<Event> findByLocationId(Long locationId);

    List<Event> findByActiveTrue();

    List<Event> findByLocationInstitutionId(Long institutionId);
}