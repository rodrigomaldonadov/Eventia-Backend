package com.backend.eventia.service;

import com.backend.eventia.dto.event.EventDetailResponseDto;
import com.backend.eventia.dto.event.EventRequestDto;
import com.backend.eventia.dto.event.EventSummaryResponseDto;
import com.backend.eventia.dto.tickettype.ValidateTicketsRequestDto;
import com.backend.eventia.dto.tickettype.ValidateTicketsResponseDto;
import com.backend.eventia.model.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface EventService {

    Page<EventSummaryResponseDto> findAll(boolean includeInactive, Pageable pageable);

    EventDetailResponseDto findById(Long id);

    Page<EventSummaryResponseDto> findByOrganizer(String organizerEmail, boolean includeInactive, Pageable pageable);

    EventDetailResponseDto create(EventRequestDto request, String organizerEmail);

    EventDetailResponseDto update(Long id, EventRequestDto request, String userEmail);

    void updateStatus(Long id, EventStatus status, String userEmail);

    void updateActive(Long id, boolean active, String userEmail);

    ValidateTicketsResponseDto validateTickets(Long eventId, ValidateTicketsRequestDto request);
}