package com.doctutorials.crm.team;

import com.doctutorials.crm.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    public record CreateTeamRequest(@NotBlank String name, String description) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Team>> create(@RequestBody CreateTeamRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Team created", teamService.create(req.name(), req.description())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Team>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Team>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(teamService.listAll()));
    }
}
