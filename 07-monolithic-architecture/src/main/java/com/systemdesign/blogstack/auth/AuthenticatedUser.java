package com.systemdesign.blogstack.auth;

import java.util.UUID;

/**
 * The shape of the authenticated principal attached to a request once the JWT filter has
 * validated the token. Mirrors {@code AuthenticatedUser} in {@code auth.types.ts}.
 */
public record AuthenticatedUser(UUID userId, String email) {
}
