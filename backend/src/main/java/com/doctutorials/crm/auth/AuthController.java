package com.doctutorials.crm.auth;

import com.doctutorials.crm.auth.dto.AuthResponse;
import com.doctutorials.crm.auth.dto.ForgotPasswordRequest;
import com.doctutorials.crm.auth.dto.LoginRequest;
import com.doctutorials.crm.auth.dto.RefreshTokenRequest;
import com.doctutorials.crm.auth.dto.ResetPasswordRequest;
import com.doctutorials.crm.auth.dto.SignupRequest;
import com.doctutorials.crm.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String GENERIC_RESET_MESSAGE = "If an account exists for that email, a reset link has been sent.";

    private final AuthService authService;
    private final TenantSignupService tenantSignupService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Workspace created", tenantSignupService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Logged out", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        // Deliberately identical response whether or not the email exists.
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(ApiResponse.ok(GENERIC_RESET_MESSAGE, null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset. Please log in again.", null));
    }
}