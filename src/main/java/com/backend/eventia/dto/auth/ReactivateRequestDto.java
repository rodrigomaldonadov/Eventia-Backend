package com.backend.eventia.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ReactivateRequestDto(
        @NotBlank @Email String email,
        @NotBlank String dni
) {
}
