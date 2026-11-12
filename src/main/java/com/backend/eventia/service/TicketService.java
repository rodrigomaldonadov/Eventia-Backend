package com.backend.eventia.service;

import com.backend.eventia.dto.order.OrderRequestDto;
import com.backend.eventia.dto.order.OrderResponseDto;
import com.backend.eventia.dto.ticket.NominateTicketRequestDto;
import com.backend.eventia.dto.ticket.TicketResponseDto;
import com.backend.eventia.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TicketService {

    OrderResponseDto purchase(OrderRequestDto requestDto, String userEmail);

    List<TicketResponseDto> findMyTickets(String userEmail);

    Page<OrderResponseDto> findOrders(OrderStatus status, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    TicketResponseDto nominate(Long ticketId, NominateTicketRequestDto request, String userEmail);
}
