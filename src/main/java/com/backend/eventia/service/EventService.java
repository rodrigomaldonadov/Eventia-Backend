package com.backend.eventia.service;

import com.backend.eventia.dto.event.EventDetailResponseDto;
import com.backend.eventia.dto.event.EventRequestDto;
import com.backend.eventia.dto.event.EventSummaryResponseDto;
import com.backend.eventia.model.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface EventService {

    Page<EventSummaryResponseDto> findAll(boolean includeInactive, Pageable pageable);

    /**
     * Búsqueda del catálogo con filtros opcionales (Integrante 4 · RF-04).
     * Filtros: q, categoryId, dateFrom, dateTo, city.
     */
    Page<EventSummaryResponseDto> findWithFilters(
            String q,
            Long categoryId,
            LocalDate dateFrom,
            LocalDate dateTo,
            String city,
            Pageable pageable
    );

    EventDetailResponseDto findById(Long id);

    Page<EventSummaryResponseDto> findByOrganizer(String organizerEmail, boolean includeInactive, Pageable pageable);

    EventDetailResponseDto create(EventRequestDto request, String organizerEmail);

    EventDetailResponseDto update(Long id, EventRequestDto request, String userEmail);

    void updateStatus(Long id, EventStatus status, String userEmail);

    void updateActive(Long id, boolean active, String userEmail);
}