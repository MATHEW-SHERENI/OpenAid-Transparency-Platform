package com.mathewshereni.open_aid_transparency.user;

import java.time.Instant;

/**
 * Output shape for a user. Critically, there is NO password / passwordHash field
 * here - we must never expose credentials over the API, not even the hash.
 */
public record AppUserResponse(
        Long id,
        String username,
        String email,
        UserRole role,
        boolean enabled,
        Instant createdAt
) {
}
