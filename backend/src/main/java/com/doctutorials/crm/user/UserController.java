package com.doctutorials.crm.user;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.user.dto.RegisterRequest;
import com.doctutorials.crm.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin-only user management. This is deliberately separate from
 * AuthController's public /register endpoint: this is the ONLY place a user
 * can be created with an elevated role (MANAGER/ADMIN/SUPER_ADMIN), and it
 * requires the caller to already be an admin - closing the privilege
 * escalation gap where public self-registration could otherwise pick its
 * own role.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody RegisterRequest request) {
        User user = userService.createByAdmin(request);
        return ResponseEntity.ok(ApiResponse.ok("User created", UserResponse.from(user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(userService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(userService.getById(id))));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        userService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("User deactivated", null));
    }
}