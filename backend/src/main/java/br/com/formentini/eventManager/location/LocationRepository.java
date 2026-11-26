package br.com.formentini.eventManager.location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface LocationRepository
        extends JpaRepository<Location, Long> {

    List<Location> findByInstitutionId(Long institutionId);
    Page<Location> findByInstitutionId(Long institutionId, Pageable pageable);
}