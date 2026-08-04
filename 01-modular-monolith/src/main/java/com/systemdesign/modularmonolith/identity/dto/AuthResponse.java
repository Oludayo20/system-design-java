package com.systemdesign.modularmonolith.identity.dto;

import com.systemdesign.modularmonolith.identity.UserRole;

import java.util.List;
import java.util.UUID;

/** Mirrors the {@code AuthResult} interface returned by {@code identity.service.ts}. */
public record AuthResponse(String accessToken, UserSummary user) {

    public record UserSummary(UUID id, String email, String fullName, List<UserRole> roles) {
    }
}
