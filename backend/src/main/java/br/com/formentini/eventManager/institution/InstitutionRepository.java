package br.com.formentini.eventManager.institution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface InstitutionRepository
        extends JpaRepository<Institution, Long> {

    List<Institution> findByTypeInstitutionId(Long typeInstitutionId);
    Page<Institution> findByTypeInstitutionId(Long typeInstitutionId, Pageable pageable);
}