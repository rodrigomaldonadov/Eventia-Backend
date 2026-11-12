package com.backend.eventia.dto.event;

import com.backend.eventia.model.enums.EventStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record EventSummaryResponseDto(
        Long id,
        String title,
        String description,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        Integer capacity,
        String bannerUrl,
        String categoryName,
        EventStatus status,
        boolean active,
        Integer availableTickets,
        Double minPrice) {
}
