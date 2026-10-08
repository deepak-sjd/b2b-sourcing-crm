package com.doctutorials.crm.document;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import com.doctutorials.crm.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Document>> upload(
            @RequestParam(required = false) UUID leadId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) UUID requirementId,
            @RequestParam(required = false) UUID quotationId,
            @RequestParam(required = false) UUID enquiryReplyId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) DocumentType type,
            @RequestParam(required = false) String description) {

        long provided = Stream.of(leadId, customerId, supplierId, requirementId, quotationId, enquiryReplyId)
                .filter(Objects::nonNull).count();
        if (provided != 1) {
            throw new BadRequestException(
                    "Exactly one of leadId, customerId, supplierId, requirementId, quotationId, enquiryReplyId is required");
        }

        DocumentParentType parentType;
        UUID parentId;
        if (leadId != null) { parentType = DocumentParentType.LEAD; parentId = leadId; }
        else if (customerId != null) { parentType = DocumentParentType.CUSTOMER; parentId = customerId; }
        else if (supplierId != null) { parentType = DocumentParentType.SUPPLIER; parentId = supplierId; }
        else if (requirementId != null) { parentType = DocumentParentType.REQUIREMENT; parentId = requirementId; }
        else if (quotationId != null) { parentType = DocumentParentType.QUOTATION; parentId = quotationId; }
        else { parentType = DocumentParentType.ENQUIRY_REPLY; parentId = enquiryReplyId; }

        Document document = documentService.upload(parentType, parentId, file, type, description);
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded", document));
    }

    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<ApiResponse<PageResponse<Document>>> byLead(@PathVariable UUID leadId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(documentService.listByLead(leadId, pageable))));
    }

    @GetMapping("/by/{parentType}/{parentId}")
    public ResponseEntity<ApiResponse<PageResponse<Document>>> byParent(
            @PathVariable DocumentParentType parentType, @PathVariable UUID parentId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(documentService.listByParent(parentType, parentId, pageable))));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        Document document = documentService.getById(id);
        Resource content = documentService.loadContent(document);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.getOriginalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(content);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        documentService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.ok("Document deleted", null));
    }
}