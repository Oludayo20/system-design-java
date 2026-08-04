package com.systemdesign.modularmonolith.identity;

import java.util.List;
import java.util.UUID;

/**
 * The shape of the authenticated principal attached to a request once the JWT filter has
 * validated the token. Mirrors {@code AuthenticatedUser} in identity.types.ts.
 */
public record AuthenticatedUser(UUID userId, String email, List<UserRole> roles) {
}
