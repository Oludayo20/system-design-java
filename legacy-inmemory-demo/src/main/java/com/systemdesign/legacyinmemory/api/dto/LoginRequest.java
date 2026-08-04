package com.systemdesign.legacyinmemory.api.dto;

/** Request body for {@code POST /auth/login}. */
public record LoginRequest(String email, String password) {
}
