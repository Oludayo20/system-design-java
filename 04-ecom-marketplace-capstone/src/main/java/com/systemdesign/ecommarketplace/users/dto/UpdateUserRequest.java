package com.systemdesign.ecommarketplace.users.dto;

import jakarta.validation.constraints.Size;

/** Mirrors src/modules/users/dto/update-user.dto.ts. fullName is optional. */
public record UpdateUserRequest(@Size(min = 2) String fullName) {}
