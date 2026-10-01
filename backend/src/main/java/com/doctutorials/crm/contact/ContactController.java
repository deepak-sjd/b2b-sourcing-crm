package com.doctutorials.crm.contact;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import com.doctutorials.crm.contact.dto.ContactRequest;
import com.doctutorials.crm.contact.dto.ContactResponse;
import com.doctutorials.crm.contact.dto.ContactStatsResponse;
import com.doctutorials.crm.contact.dto.DuplicateCheckResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<ContactResponse>> create(@Valid @RequestBody ContactRequest request) {
        Contact contact = contactService.create(request);
        return ResponseEntity.ok(ApiResponse.ok("Contact created", ContactResponse.from(contact)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(ContactResponse.from(contactService.getById(id))));
    }

    // Default sort mirrors LeadController's rationale: newest-first with id as a
    // tie-breaker so paging stays stable and a just-created contact never seems
    // to vanish into an unordered later page.
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ContactResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ContactEngagement engagement,
            @RequestParam(required = false) UUID ownerId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Contact> page = contactService.list(search, engagement, ownerId, pageable);
        Page<ContactResponse> mapped = page.map(ContactResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(mapped)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ContactStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(contactService.stats()));
    }

    /** Powers the live "possible duplicate" hint on the create-contact form. */
    @GetMapping("/check-duplicate")
    public ResponseEntity<ApiResponse<DuplicateCheckResponse>> checkDuplicate(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UUID excludeId) {
        return ResponseEntity.ok(ApiResponse.ok(contactService.checkDuplicate(phone, email, excludeId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<ContactResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ContactRequest request) {
        Contact contact = contactService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Contact updated", ContactResponse.from(contact)));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<ContactResponse>> assign(@PathVariable UUID id, @RequestParam UUID ownerId) {
        Contact contact = contactService.assignOwner(id, ownerId);
        return ResponseEntity.ok(ApiResponse.ok("Contact reassigned", ContactResponse.from(contact)));
    }

    @PostMapping("/{id}/touchpoint")
    public ResponseEntity<ApiResponse<ContactResponse>> recordTouchpoint(@PathVariable UUID id) {
        Contact contact = contactService.recordTouchpoint(id);
        return ResponseEntity.ok(ApiResponse.ok("Activity logged", ContactResponse.from(contact)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> archive(@PathVariable UUID id) {
        contactService.archive(id);
        return ResponseEntity.ok(ApiResponse.ok("Contact archived", null));
    }
}
