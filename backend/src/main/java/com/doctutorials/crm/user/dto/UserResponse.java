package com.doctutorials.crm.user.dto;

import com.doctutorials.crm.user.Role;
import com.doctutorials.crm.user.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        String teamName,
        boolean active
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getRole(),
                u.getTeam() != null ? u.getTeam().getName() : null,
                u.isActive()
        );
    }
}
