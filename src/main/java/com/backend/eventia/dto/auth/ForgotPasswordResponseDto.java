package com.backend.eventia.dto.auth;

/**
 * Respuesta con el código de recuperación. En un entorno real el código
 * se enviaría por correo; aquí se devuelve para poder completar el flujo
 * en la app de demostración.
 */
public record ForgotPasswordResponseDto(
        String email,
        String code,
        int expiresInMinutes,
        String message
) {
}
