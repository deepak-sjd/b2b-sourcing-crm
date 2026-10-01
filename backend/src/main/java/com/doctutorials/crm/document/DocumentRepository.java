package com.doctutorials.crm.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByLeadIdOrderByCreatedAtDesc(UUID leadId, Pageable pageable);
}
