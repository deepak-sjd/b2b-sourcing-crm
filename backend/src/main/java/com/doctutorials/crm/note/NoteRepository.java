package com.doctutorials.crm.note;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    Page<Note> findByLeadIdOrderByCreatedAtDesc(UUID leadId, Pageable pageable);
}
