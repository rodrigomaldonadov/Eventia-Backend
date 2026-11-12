package com.backend.eventia.repository;

import com.backend.eventia.model.Event;
import com.backend.eventia.model.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Consultas del modulo Admin

    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN FETCH e.category
            JOIN FETCH e.organizer
            LEFT JOIN FETCH e.ticketTypes
            WHERE e.active = true
            AND e.status NOT IN (com.backend.eventia.model.enums.EventStatus.PENDING, com.backend.eventia.model.enums.EventStatus.CANCELLED)
            ORDER BY e.date ASC, e.startTime ASC
            """)
    List<Event> findActiveEventsWithDetails();

    List<Event> findByOrganizerIdAndActiveTrue(Long organizerId);


    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    Page<Event> findByOrganizerIdAndActiveTrue(Long organizerId, Pageable pageable);


    Page<Event> findByActiveTrueAndStatus(EventStatus status, Pageable pageable);

    boolean existsByTitleIgnoreCaseAndOrganizerId(String title, Long organizerId);

    /**
     * Búsqueda del catálogo público con filtros opcionales (Integrante 4 · RF-04).
     * Filtros: q (texto en título/descripción/ubicación), categoryId, dateFrom, dateTo, city.
     * Solo devuelve eventos activos y publicados.
     */
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN FETCH e.category
            JOIN FETCH e.organizer
            LEFT JOIN FETCH e.ticketTypes
            WHERE e.active = true
            AND e.status = com.backend.eventia.model.enums.EventStatus.PUBLISHED
            AND (:q IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(e.location) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:categoryId IS NULL OR e.category.id = :categoryId)
            AND (:dateFrom IS NULL OR e.date >= :dateFrom)
            AND (:dateTo IS NULL OR e.date <= :dateTo)
            AND (:city IS NULL OR LOWER(e.location) LIKE LOWER(CONCAT('%', :city, '%')))
            ORDER BY e.date ASC, e.startTime ASC
            """)
    Page<Event> findWithFilters(
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("city") String city,
            Pageable pageable
    );
}
