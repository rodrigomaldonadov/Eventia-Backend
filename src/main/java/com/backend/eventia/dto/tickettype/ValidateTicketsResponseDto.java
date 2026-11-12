package com.backend.eventia.dto.tickettype;

/**
 * Resultado de validar disponibilidad de tarifas.
 * POST /api/events/{eventId}/validate-tickets
 */
public record ValidateTicketsResponseDto(
        boolean valid,
        Long eventId,
        double subtotal,
        int totalTickets,
        String message
) {
}
