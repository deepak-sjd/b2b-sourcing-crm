package com.doctutorials.crm.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID>, JpaSpecificationExecutor<Activity> {
    Page<Activity> findByLeadId(UUID leadId, Pageable pageable);
    // No count/aggregate finders here on purpose - ActivityService builds every
    // number itself via ActivityAccessService so every figure is scoped to what
    // the caller can see (same reasoning as the original CallRepository).
}