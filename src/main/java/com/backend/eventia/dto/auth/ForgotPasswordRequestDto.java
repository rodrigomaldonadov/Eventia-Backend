package com.backend.eventia.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload para solicitar un código de recuperación de contraseña.
 * POST /auth/forgot-password
 */
public record ForgotPasswordRequestDto(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String email
) {
}
