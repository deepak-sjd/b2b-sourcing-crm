package com.doctutorials.crm.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for the PUBLIC, unauthenticated /api/v1/auth/register endpoint.
 *
 * Deliberately has NO role or teamId field. Self-registered accounts are
 * always created as COUNSELOR with no team, full stop — there is no way
 * for an anonymous caller to request a different role. Elevated roles
 * (MANAGER/ADMIN/SUPER_ADMIN) can only be granted via the admin-only
 * POST /api/v1/users endpoint (see UserController + RegisterRequest),
 * which requires an authenticated ADMIN/SUPER_ADMIN caller.
 */
public record PublicRegisterRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password
) {}