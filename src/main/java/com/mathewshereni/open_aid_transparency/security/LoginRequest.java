package com.mathewshereni.open_aid_transparency.security;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "username is required")
        String username,

        @NotBlank(message = "password is required")
        String password
) {
}
