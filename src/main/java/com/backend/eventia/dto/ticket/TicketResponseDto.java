package com.backend.eventia.dto.ticket;

import com.backend.eventia.model.enums.TicketStatus;

import java.time.LocalDateTime;

public record TicketResponseDto(
        Long id,
        Long orderId,
        Long eventId,
        String eventTitle,
        String ticketType,
        LocalDateTime eventDate,
        String venue,
        TicketStatus status,
        String nombre,
        String dni,
        String qrCode
) {
}
