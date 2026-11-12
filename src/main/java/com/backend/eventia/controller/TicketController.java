package com.backend.eventia.controller;

import com.backend.eventia.dto.order.OrderRequestDto;
import com.backend.eventia.dto.order.OrderResponseDto;
import com.backend.eventia.dto.ticket.NominateTicketRequestDto;
import com.backend.eventia.dto.ticket.TicketResponseDto;
import com.backend.eventia.model.enums.OrderStatus;
import com.backend.eventia.service.TicketService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/api/orders")
    public ResponseEntity<OrderResponseDto> purchase(
            @RequestBody @Valid OrderRequestDto request,
            Authentication authentication) {
        log.info("[TicketController] POST /api/orders - usuario='{}'", authentication.getName());
        OrderResponseDto response = ticketService.purchase(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/orders")
    public ResponseEntity<Page<OrderResponseDto>> findOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("[TicketController] GET /api/orders - status='{}', fromDate='{}', toDate='{}'", status, fromDate,
                toDate);
        return ResponseEntity.ok(ticketService.findOrders(status, fromDate, toDate, pageable));
    }

    @GetMapping("/api/tickets/my")
    public ResponseEntity<List<TicketResponseDto>> findMyTickets(Authentication authentication) {
        log.debug("[TicketController] GET /api/tickets/my - usuario='{}'", authentication.getName());
        return ResponseEntity.ok(ticketService.findMyTickets(authentication.getName()));
    }

    @PatchMapping("/api/tickets/{id}/nominate")
    public ResponseEntity<TicketResponseDto> nominate(
            @PathVariable Long id,
            @RequestBody @Valid NominateTicketRequestDto request,
            Authentication authentication
    ) {
        log.info("[TicketController] PATCH /api/tickets/{}/nominate - usuario='{}'", id, authentication.getName());
        return ResponseEntity.ok(ticketService.nominate(id, request, authentication.getName()));
    }
}
