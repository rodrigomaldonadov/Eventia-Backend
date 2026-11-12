package com.backend.eventia.dto.tickettype;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Payload para validar disponibilidad de tarifas antes de crear la orden.
 * POST /api/events/{eventId}/validate-tickets
 */
public record ValidateTicketsRequestDto(
        @NotEmpty(message = "Debe seleccionar al menos una entrada")
        List<@NotNull(message = "El ítem no puede ser nulo") @Valid Item> items
) {
    public record Item(
            @NotNull(message = "La tarifa es obligatoria")
            Long ticketId,

            @NotNull(message = "La cantidad es obligatoria")
            @Positive(message = "La cantidad debe ser mayor a cero")
            Integer quantity
    ) {
    }
}
