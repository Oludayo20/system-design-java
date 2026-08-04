package com.systemdesign.ecommarketplace.auth.dto;

/** Mirrors AuthService.register/login's return shape: { accessToken, user }. */
public record AuthResponse(String accessToken, AuthUserSummary user) {}
