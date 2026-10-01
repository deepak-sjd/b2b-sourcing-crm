package com.doctutorials.crm.requirement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface RequirementRepository extends JpaRepository<Requirement, UUID>, JpaSpecificationExecutor<Requirement> {

    // Fetch the associations the response DTO reads so mapping never triggers lazy loads
    // (open-in-view is off).
    @EntityGraph(attributePaths = { "lead", "assignedTo" })
    Page<Requirement> findByLeadId(UUID leadId, Pageable pageable);
}
