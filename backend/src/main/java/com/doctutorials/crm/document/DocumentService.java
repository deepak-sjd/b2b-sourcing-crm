package com.doctutorials.crm.document;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.customer.CustomerService;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadService;
import com.doctutorials.crm.quotation.QuotationService;
import com.doctutorials.crm.requirement.Requirement;
import com.doctutorials.crm.requirement.RequirementRepository;
import com.doctutorials.crm.requirement.RequirementService;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.storage.FileStorageService;
import com.doctutorials.crm.supplier.SupplierService;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiryReply;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiryReplyRepository;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final LeadService leadService;
    private final CustomerService customerService;
    private final SupplierService supplierService;
    private final RequirementService requirementService;
    private final RequirementRepository requirementRepository;
    private final QuotationService quotationService;
    private final SupplierEnquiryService supplierEnquiryService;
    private final SupplierEnquiryReplyRepository supplierEnquiryReplyRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    @Value("${app.storage.max-file-size-mb}")
    private long maxFileSizeMb;

    // extension -> acceptable declared Content-Type(s). Allowlist, not a
    // denylist, same reasoning as before V55.
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "pdf", Set.of("application/pdf"),
            "doc", Set.of("application/msword"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "xls", Set.of("application/vnd.ms-excel"),
            "xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            "csv", Set.of("text/csv", "application/vnd.ms-excel"),
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "txt", Set.of("text/plain"));

    @Transactional
    public Document upload(DocumentParentType parentType, UUID parentId, MultipartFile file, DocumentType type, String description) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided");
        }
        if (file.getSize() > maxFileSizeMb * 1024L * 1024L) {
            throw new BadRequestException("File exceeds the " + maxFileSizeMb + "MB limit");
        }

        String originalName = sanitizeDisplayName(file.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
        String contentType = file.getContentType();
        validateFile(originalName, contentType, bytes);

        Document document = new Document();
        applyParent(document, parentType, parentId);
        document.setUploadedBy(currentUserService.get());
        document.setDocumentType(type != null ? type : DocumentType.OTHER);
        document.setOriginalFileName(originalName);
        document.setContentType(contentType);
        document.setSizeBytes(bytes.length);
        document.setDescription(blankToNull(description));

        FileStorageService.StoredFile stored = fileStorageService.store(parentId, originalName, contentType,
                new ByteArrayInputStream(bytes), bytes.length);
        document.setStorageKey(stored.storageKey());

        Document saved = documentRepository.save(document);
        auditService.log("Document", saved.getId(), AuditAction.CREATE,
                "Document uploaded: " + originalName + " (" + parentType + ")");
        return saved;
    }

    /** Resolves the parent via its own service's getById - that's the
     *  visibility check, same pattern every other module uses. */
    private void applyParent(Document document, DocumentParentType parentType, UUID parentId) {
        switch (parentType) {
            case LEAD -> {
                Lead lead = leadService.getById(parentId);
                document.setLead(lead);
            }
            case CUSTOMER -> document.setCustomer(customerService.getById(parentId));
            case SUPPLIER -> document.setSupplier(supplierService.getById(parentId));
            case REQUIREMENT -> {
                requirementService.getById(parentId); // visibility check (DTO result discarded)
                Requirement requirement = requirementRepository.findById(parentId)
                        .orElseThrow(() -> ResourceNotFoundException.of("Requirement", parentId));
                document.setRequirement(requirement);
            }
            case QUOTATION -> document.setQuotation(quotationService.getById(parentId));
            case ENQUIRY_REPLY -> {
                SupplierEnquiryReply reply = supplierEnquiryReplyRepository.findById(parentId)
                        .orElseThrow(() -> ResourceNotFoundException.of("SupplierEnquiryReply", parentId));
                supplierEnquiryService.getById(reply.getEnquiry().getId()); // visibility via the enquiry's lead
                document.setEnquiryReply(reply);
            }
        }
    }

    private void validateFile(String originalName, String contentType, byte[] bytes) {
        String ext = extensionOf(originalName);
        Set<String> acceptableTypes = ALLOWED.get(ext);
        if (acceptableTypes == null) {
            throw new BadRequestException("Unsupported file extension: ." + ext);
        }
        if (contentType == null || !acceptableTypes.contains(contentType)) {
            throw new BadRequestException("File extension ." + ext + " doesn't match its content type"
                    + (contentType != null ? " (" + contentType + ")" : ""));
        }
        if (!signatureMatches(ext, bytes)) {
            throw new BadRequestException("File content doesn't match its extension (." + ext + ") - possible spoofed file type");
        }
    }

    // Magic-byte check per extension - see DELIVERABLE assumption #2 for why
    // this is hand-rolled rather than a sniffing library.
    private boolean signatureMatches(String ext, byte[] bytes) {
        return switch (ext) {
            case "pdf" -> startsWith(bytes, 0x25, 0x50, 0x44, 0x46); // %PDF
            case "png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47);
            case "jpg", "jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
            case "docx", "xlsx" -> startsWith(bytes, 0x50, 0x4B, 0x03, 0x04); // PK.. (zip-based OOXML)
            case "doc", "xls" -> startsWith(bytes, 0xD0, 0xCF, 0x11, 0xE0); // legacy OLE2
            case "csv", "txt" -> true; // no reliable binary signature for plain text
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, int... expected) {
        if (bytes.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[i] & 0xFF) != expected[i]) return false;
        }
        return true;
    }

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    public Page<Document> listByParent(DocumentParentType parentType, UUID parentId, Pageable pageable) {
        return switch (parentType) {
            case LEAD -> {
                leadService.getById(parentId);
                yield documentRepository.findByLeadIdAndDeletedAtIsNullOrderByCreatedAtDesc(parentId, pageable);
            }
            case CUSTOMER -> {
                customerService.getById(parentId);
                yield documentRepository.findByCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(parentId, pageable);
            }
            case SUPPLIER -> {
                supplierService.getById(parentId);
                yield documentRepository.findBySupplierIdAndDeletedAtIsNullOrderByCreatedAtDesc(parentId, pageable);
            }
            case REQUIREMENT -> {
                requirementService.getById(parentId);
                yield documentRepository.findByRequirementIdAndDeletedAtIsNullOrderByCreatedAtDesc(parentId, pageable);
            }
            case QUOTATION -> {
                quotationService.getById(parentId);
                yield documentRepository.findByQuotationIdAndDeletedAtIsNullOrderByCreatedAtDesc(parentId, pageable);
            }
            case ENQUIRY_REPLY -> {
                SupplierEnquiryReply reply = supplierEnquiryReplyRepository.findById(parentId)
                        .orElseThrow(() -> ResourceNotFoundException.of("SupplierEnquiryReply", parentId));
                supplierEnquiryService.getById(reply.getEnquiry().getId());
                yield documentRepository.findByEnquiryReplyIdAndDeletedAtIsNullOrderByCreatedAtDesc(parentId, pageable);
            }
        };
    }

    /** Kept working exactly as before - a thin wrapper over listByParent(LEAD, ...). */
    public Page<Document> listByLead(UUID leadId, Pageable pageable) {
        return listByParent(DocumentParentType.LEAD, leadId, pageable);
    }

    @Transactional(readOnly = true)
    public Document getById(UUID id) {
        Document document = documentRepository.findById(id)
                .filter(d -> d.getDeletedAt() == null)
                .orElseThrow(() -> ResourceNotFoundException.of("Document", id));
        assertParentVisible(document);
        return document;
    }

    private void assertParentVisible(Document document) {
        if (document.getLead() != null) leadService.getById(document.getLead().getId());
        else if (document.getCustomer() != null) customerService.getById(document.getCustomer().getId());
        else if (document.getSupplier() != null) supplierService.getById(document.getSupplier().getId());
        else if (document.getRequirement() != null) requirementService.getById(document.getRequirement().getId());
        else if (document.getQuotation() != null) quotationService.getById(document.getQuotation().getId());
        else if (document.getEnquiryReply() != null) {
            supplierEnquiryService.getById(document.getEnquiryReply().getEnquiry().getId());
        }
    }

    public Resource loadContent(Document document) {
        return fileStorageService.load(document.getStorageKey());
    }

    @Transactional
    public void softDelete(UUID id) {
        Document document = getById(id);
        document.setDeletedAt(Instant.now());
        document.setDeletedBy(currentUserService.get());
        documentRepository.save(document);
        auditService.log("Document", id, AuditAction.DELETE, "Document soft-deleted: " + document.getOriginalFileName());
    }

    private String sanitizeDisplayName(String rawName) {
        if (rawName == null || rawName.isBlank()) return "file";
        String cleaned = rawName.replaceAll("[\\r\\n\"/\\\\]", "_");
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}