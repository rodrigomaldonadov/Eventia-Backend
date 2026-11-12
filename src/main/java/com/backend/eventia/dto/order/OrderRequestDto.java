package com.backend.eventia.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderRequestDto(
        @NotBlank(message = "El método de pago es obligatorio")
        String paymentMethod,

        @NotEmpty(message = "Debe seleccionar al menos una entrada")
        List<@NotNull(message = "El ítem no puede ser nulo") @Valid Item> items
) {
    public record Item(
            @NotNull(message = "La tarifa es obligatoria")
            Long ticketTypeId,

            @NotNull(message = "La cantidad es obligatoria")
            @Positive(message = "La cantidad debe ser mayor a cero")
            Integer quantity,

            String nombre,
            String dni,
            List<Attendee> attendees
    ) {
        public record Attendee(String nombre, String dni) {
        }
    }

}
