package com.backend.eventia.controller;

import com.backend.eventia.dto.event.EventDetailResponseDto;
import com.backend.eventia.dto.event.EventRequestDto;
import com.backend.eventia.dto.event.EventSummaryResponseDto;
import com.backend.eventia.model.enums.EventStatus;
import com.backend.eventia.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventSummaryResponseDto>> findAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String city,
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.debug("[EventController] GET /api/events - Listando catálogo público de eventos (q={}, categoryId={}, dateFrom={}, dateTo={}, city={}, page={}, size={})",
                q, categoryId, dateFrom, dateTo, city, pageable.getPageNumber(), pageable.getPageSize());

        Page<EventSummaryResponseDto> events = eventService.findWithFilters(
                q, categoryId, dateFrom, dateTo, city, pageable
        );
        return ResponseEntity.ok(events);
    }

    @GetMapping("/organizer")
    public ResponseEntity<Page<EventSummaryResponseDto>> findMyEvents(
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 10, sort = "date", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        log.debug("[EventController] GET /api/events/organizer - Listando eventos del organizador '{}' (includeInactive={}, page={}, size={})",
                authentication.getName(), includeInactive, pageable.getPageNumber(), pageable.getPageSize());

        Page<EventSummaryResponseDto> events = eventService.findByOrganizer(authentication.getName(), includeInactive, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailResponseDto> findById(@PathVariable Long id) {
        log.debug("[EventController] GET /api/events/{} - Consultando detalle de evento por ID", id);

        EventDetailResponseDto event = eventService.findById(id);
        return ResponseEntity.ok(event);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventDetailResponseDto> create(
            @RequestBody @Valid EventRequestDto request,
            Authentication authentication
    ) {
        log.info("[EventController] POST /api/events - Creando nuevo evento título='{}' por usuario='{}'",
                request.title(), authentication.getName());

        EventDetailResponseDto createdEvent = eventService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<EventDetailResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid EventRequestDto request,
            Authentication authentication
    ) {
        log.info("[EventController] PUT /api/events/{} - Actualizando evento título='{}' por usuario='{}'",
                id, request.title(), authentication.getName());

        EventDetailResponseDto updatedEvent = eventService.update(id, request, authentication.getName());
        return ResponseEntity.ok(updatedEvent);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam EventStatus status,
            Authentication authentication
    ) {
        log.info("[EventController] PATCH /api/events/{}/status - Cambiando estado a status={} por usuario='{}'",
                id, status, authentication.getName());

        eventService.updateStatus(id, status, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<Void> updateActive(
            @PathVariable Long id,
            @RequestParam boolean active,
            Authentication authentication
    ) {
        log.info("[EventController] PATCH /api/events/{}/active - Cambiando estado activo a active={} por usuario='{}'",
                id, active, authentication.getName());

        eventService.updateActive(id, active, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}