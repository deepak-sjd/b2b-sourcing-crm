package com.doctutorials.crm.document;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final LeadService leadService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    @Value("${app.storage.max-file-size-mb}")
    private long maxFileSizeMb;

    // Deliberately an allowlist, not a denylist - a new file type is
    // invisible/rejected by
    // default rather than silently accepted until someone remembers to block it.
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @Transactional
    public Document upload(UUID leadId, MultipartFile file, DocumentType type) {
        // LeadService.getById - throws 404 if the caller can't see this lead, so a file
        // can't be attached to a lead outside their scope.
        Lead lead = leadService.getById(leadId);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided");
        }
        if (file.getSize() > maxFileSizeMb * 1024L * 1024L) {
            throw new BadRequestException("File exceeds the " + maxFileSizeMb + "MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(
                    "Unsupported file type" + (contentType != null ? ": " + contentType : "") +
                            ". Allowed: PDF, JPEG, PNG, Word, Excel");
        }

        String originalName = sanitizeDisplayName(file.getOriginalFilename());

        FileStorageService.StoredFile stored;
        try {
            stored = fileStorageService.store(leadId, originalName, contentType, file.getInputStream(), file.getSize());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }

        Document document = new Document();
        document.setLead(lead);
        document.setUploadedBy(currentUserService.get());
        document.setDocumentType(type != null ? type : DocumentType.OTHER);
        document.setOriginalFileName(originalName);
        document.setStorageKey(stored.storageKey());
        document.setContentType(contentType);
        document.setSizeBytes(stored.sizeBytes());

        Document saved = documentRepository.save(document);
        auditService.log("Document", saved.getId(), AuditAction.CREATE,
                "Document uploaded: " + originalName);
        return saved;
    }

    public Page<Document> listByLead(UUID leadId, Pageable pageable) {
        leadService.getById(leadId); // 404s if not visible; every document under it is then in scope
        return documentRepository.findByLeadIdOrderByCreatedAtDesc(leadId, pageable);
    }

    public Document getById(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Document", id));
        // A document is visible exactly when its lead is - re-running the lead's own
        // visibility check keeps this in sync with LeadAccessService automatically.
        leadService.getById(document.getLead().getId());
        return document;
    }

    public Resource loadContent(Document document) {
        return fileStorageService.load(document.getStorageKey());
    }

    @Transactional
    public void delete(UUID id) {
        Document document = getById(id);
        fileStorageService.delete(document.getStorageKey());
        documentRepository.delete(document);
        auditService.log("Document", id, AuditAction.DELETE, "Document deleted: " + document.getOriginalFileName());
    }

    /**
     * Strips anything that isn't safe to echo back later (e.g. in a
     * Content-Disposition header).
     */
    private String sanitizeDisplayName(String rawName) {
        if (rawName == null || rawName.isBlank())
            return "file";
        String cleaned = rawName.replaceAll("[\\r\\n\"]", "_");
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }
}
