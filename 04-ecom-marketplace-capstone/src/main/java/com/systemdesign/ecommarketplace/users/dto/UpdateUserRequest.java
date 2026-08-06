package com.systemdesign.ecommarketplace.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Partial profile update. Only provided fields are changed.")
public record UpdateUserRequest(@Schema(example = "Ada Lovelace") @Size(min = 2) String fullName) {}
