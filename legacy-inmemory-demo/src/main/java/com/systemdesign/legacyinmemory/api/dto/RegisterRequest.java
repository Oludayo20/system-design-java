package com.systemdesign.legacyinmemory.api.dto;

/** Request body for {@code POST /auth/register}. */
public record RegisterRequest(String email, String password) {
}
