package com.doctutorials.crm.document;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Document>> upload(
            @RequestParam UUID leadId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) DocumentType type) {
        Document document = documentService.upload(leadId, file, type);
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded", document));
    }

    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<ApiResponse<PageResponse<Document>>> byLead(@PathVariable UUID leadId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(documentService.listByLead(leadId, pageable))));
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
                .body(content);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Document deleted", null));
    }
}
