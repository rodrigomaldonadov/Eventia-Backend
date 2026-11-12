package com.backend.eventia.dto.auth;

import com.backend.eventia.model.enums.TipoDocumento;

// Perfil devuelto por GET /auth/me a partir del JWT.
public record AuthUserResponseDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role,
        TipoDocumento documentType,
        String dni,
        String cellphone
) {
}
