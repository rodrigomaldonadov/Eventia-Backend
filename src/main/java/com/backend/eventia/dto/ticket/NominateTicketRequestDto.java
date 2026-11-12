package com.backend.eventia.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NominateTicketRequestDto(
        @NotBlank(message = "El nombre del titular es obligatorio")
        @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String nombre,

        @NotBlank(message = "El DNI del titular es obligatorio")
        @Size(min = 6, max = 20, message = "El DNI o documento debe tener entre 6 y 20 caracteres")
        String dni
) {
}

