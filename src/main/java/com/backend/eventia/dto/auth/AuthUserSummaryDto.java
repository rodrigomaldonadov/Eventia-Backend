package com.backend.eventia.dto.auth;

import com.backend.eventia.model.enums.TipoDocumento;

// Datos básicos del usuario incluidos en la respuesta de login/registro.
public record AuthUserSummaryDto(
        Long id,
        String name,
        String email,
        String role,
        TipoDocumento documentType,
        String dni,
        String cellphone
) {
}
