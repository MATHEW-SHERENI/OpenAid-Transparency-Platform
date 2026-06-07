package com.mathewshereni.open_aid_transparency.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Input shape for creating a user. The raw password arrives here, gets validated,
 * and is immediately hashed in the service - it is never stored or returned as-is.
 * (BCrypt only uses the first 72 bytes, hence the max.)
 */
public record AppUserRequest(

        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be 3-50 characters")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 150, message = "email must be at most 150 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8-72 characters")
        String password,

        @NotNull(message = "role is required")
        UserRole role
) {
}
