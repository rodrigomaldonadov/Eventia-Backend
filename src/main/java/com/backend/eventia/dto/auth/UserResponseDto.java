package com.backend.eventia.dto.auth;

/**
 * DTO de respuesta del perfil de usuario (GET /auth/me, PATCH /api/users/me).
 */
public record UserResponseDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String dni,
        String cellphone,
        String role,
        boolean active
) {
}
