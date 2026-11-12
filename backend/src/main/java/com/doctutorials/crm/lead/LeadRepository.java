package com.doctutorials.crm.lead;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    // Duplicate checks for bulk import (tenant-filtered automatically).
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);
}