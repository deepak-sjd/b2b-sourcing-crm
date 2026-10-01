package com.doctutorials.crm.note;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.common.PageResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    public record AddNoteRequest(UUID leadId, UUID authorId, @NotBlank String content) {}

    @PostMapping
    public ResponseEntity<ApiResponse<Note>> add(@RequestBody AddNoteRequest req) {
        Note note = noteService.add(req.leadId(), req.authorId(), req.content());
        return ResponseEntity.ok(ApiResponse.ok("Note added", note));
    }

    @GetMapping("/by-lead/{leadId}")
    public ResponseEntity<ApiResponse<PageResponse<Note>>> byLead(@PathVariable UUID leadId,
            @PageableDefault(sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(noteService.listByLead(leadId, pageable))));
    }
}
