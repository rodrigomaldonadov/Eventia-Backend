package com.backend.eventia.repository;

import com.backend.eventia.dto.user.OrganizerCustomerResponseDto;
import com.backend.eventia.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(value = """
            SELECT new com.backend.eventia.dto.user.OrganizerCustomerResponseDto(
                u.id, u.firstName, u.lastName, u.email, u.documentType, u.dni, u.cellphone, u.active,
                COALESCE(SUM(od.quantity), 0)
            )
            FROM Order o
            JOIN o.user u
            JOIN o.orderDetails od
            JOIN od.ticketType tt
            JOIN tt.event e
            WHERE e.organizer.id = :organizerId
              AND u.role = com.backend.eventia.model.enums.UserRole.USER
              AND o.paymentStatus = com.backend.eventia.model.enums.OrderStatus.PAID
              AND (:active IS NULL OR u.active = :active)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.dni) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')))
            GROUP BY u.id, u.firstName, u.lastName, u.email, u.documentType, u.dni, u.cellphone, u.active
            ORDER BY u.firstName ASC, u.lastName ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT u.id)
            FROM Order o
            JOIN o.user u
            JOIN o.orderDetails od
            JOIN od.ticketType tt
            JOIN tt.event e
            WHERE e.organizer.id = :organizerId
              AND u.role = com.backend.eventia.model.enums.UserRole.USER
              AND o.paymentStatus = com.backend.eventia.model.enums.OrderStatus.PAID
              AND (:active IS NULL OR u.active = :active)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.dni) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<OrganizerCustomerResponseDto> findCustomersByOrganizer(
            @Param("organizerId") Long organizerId,
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query(value = """
                    SELECT o FROM Order o
                    WHERE o.user.email = :userEmail
                      AND (:status IS NULL OR o.paymentStatus = :status)
                      AND (:fromDate IS NULL OR o.createdAt >= :fromDate)
                      AND (:toDate IS NULL OR o.createdAt <= :toDate)
                    """, countQuery = """
                    SELECT COUNT(o) FROM Order o
                    WHERE o.user.email = :userEmail
                      AND (:status IS NULL OR o.paymentStatus = :status)
                      AND (:fromDate IS NULL OR o.createdAt >= :fromDate)
                      AND (:toDate IS NULL OR o.createdAt <= :toDate)
                    """)
    Page<Order> findAllFilteredByUser(
                    @Param("userEmail") String userEmail,
                    @Param("status") com.backend.eventia.model.enums.OrderStatus status,
                    @Param("fromDate") java.time.LocalDateTime fromDate,
                    @Param("toDate") java.time.LocalDateTime toDate,
                    Pageable pageable);
}
