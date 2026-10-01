package com.doctutorials.crm.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UUID userId,
        String fullName,
        String email,
        String role) {
    public static AuthResponse of(String accessToken, String refreshToken,
            UUID userId, String fullName, String email, String role) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", userId, fullName, email, role);
    }
}
