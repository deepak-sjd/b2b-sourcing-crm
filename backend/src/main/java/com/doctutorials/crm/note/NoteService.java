package com.doctutorials.crm.note;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final LeadService leadService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public Note add(UUID leadId, UUID authorId, String content) {
        // LeadService.getById - throws 404 if the caller can't see this lead.
        Lead lead = leadService.getById(leadId);

        Note note = new Note();
        note.setLead(lead);
        note.setContent(content);

        if (authorId != null) {
            User author = userRepository.findById(authorId)
                    .orElseThrow(() -> ResourceNotFoundException.of("User", authorId));
            note.setAuthor(author);
        }

        Note saved = noteRepository.save(note);
        auditService.log("Note", saved.getId(), AuditAction.CREATE, "Note added to lead");
        return saved;
    }

    public Page<Note> listByLead(UUID leadId, Pageable pageable) {
        leadService.getById(leadId); // 404s if not visible; every note under it is then in scope
        return noteRepository.findByLeadIdOrderByCreatedAtDesc(leadId, pageable);
    }
}
