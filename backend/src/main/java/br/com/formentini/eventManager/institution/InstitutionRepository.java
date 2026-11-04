package br.com.formentini.eventManager.institution;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InstitutionRepository
        extends JpaRepository<Institution, Long> {

    List<Institution> findByTypeInstitutionId(Long typeInstitutionId);
}