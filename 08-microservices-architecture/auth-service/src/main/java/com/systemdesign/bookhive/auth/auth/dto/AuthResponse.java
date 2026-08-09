package com.systemdesign.bookhive.auth.auth.dto;

import java.util.UUID;

public record AuthResponse(String accessToken, UserSummary user) {

    public record UserSummary(UUID id, String email, String fullName) {
    }
}
