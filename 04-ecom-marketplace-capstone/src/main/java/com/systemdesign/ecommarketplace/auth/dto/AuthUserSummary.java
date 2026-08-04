package com.systemdesign.ecommarketplace.auth.dto;

import java.util.UUID;

/** The narrow `user` shape AuthService.register/login return - id/email/fullName only. */
public record AuthUserSummary(UUID id, String email, String fullName) {}
