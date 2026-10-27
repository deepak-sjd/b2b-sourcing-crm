package com.doctutorials.crm.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public sign-up: creates a new tenant workspace plus its first ADMIN user. */
public record SignupRequest(
        @NotBlank @Size(min = 2, max = 100) String companyName,
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password
) {}