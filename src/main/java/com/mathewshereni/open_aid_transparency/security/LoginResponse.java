package com.mathewshereni.open_aid_transparency.security;

/**
 * What the login endpoint returns. tokenType is always "Bearer" - that's the
 * scheme clients use in the header:  Authorization: Bearer <token>
 */
public record LoginResponse(
        String token,
        String tokenType,
        String username,
        String role
) {
}
