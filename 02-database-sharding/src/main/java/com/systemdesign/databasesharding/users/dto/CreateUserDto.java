package com.systemdesign.databasesharding.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mirrors create-user.dto.ts. */
@Getter
@Setter
@NoArgsConstructor
public class CreateUserDto {

    @Schema(example = "ada@oja.africa")
    @NotBlank
    @Email
    private String email;

    @Schema(example = "Ada Lovelace")
    @NotBlank
    @Size(min = 1, max = 120)
    private String displayName;

    @Schema(example = "africa", description = "Free-form region label, e.g. africa/europe/asia")
    @NotBlank
    private String region;
}
