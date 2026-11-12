package com.backend.eventia.service.impl;

import com.backend.eventia.dto.event.EventDetailResponseDto;
import com.backend.eventia.dto.event.EventRequestDto;
import com.backend.eventia.dto.event.EventSummaryResponseDto;
import com.backend.eventia.exception.DuplicateResourceException;
import com.backend.eventia.exception.ResourceNotFoundException;
import com.backend.eventia.model.Category;
import com.backend.eventia.model.Event;
import com.backend.eventia.model.User;
import com.backend.eventia.model.enums.EventStatus;
import com.backend.eventia.model.enums.UserRole;
import com.backend.eventia.repository.CategoryRepository;
import com.backend.eventia.repository.EventRepository;
import com.backend.eventia.repository.TicketTypeRepository;
import com.backend.eventia.repository.UserRepository;
import com.backend.eventia.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TicketTypeRepository ticketTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<EventSummaryResponseDto> findAll(boolean includeInactive, Pageable pageable) {
        log.debug("[Event] findAll(includeInactive={}, page={}, size={})",
                includeInactive, pageable.getPageNumber(), pageable.getPageSize());

        // Para el catálogo general, solo se listan eventos activos y publicados
        Page<Event> events = includeInactive
                ? eventRepository.findAll(pageable)
                : eventRepository.findByActiveTrueAndStatus(EventStatus.PUBLISHED, pageable);

        log.debug("[Event] findAll -> {} eventos encontrados en página {} (includeInactive={})",
                events.getNumberOfElements(), pageable.getPageNumber(), includeInactive);
        return events.map(this::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventSummaryResponseDto> findWithFilters(
            String q,
            Long categoryId,
            LocalDate dateFrom,
            LocalDate dateTo,
            String city,
            Pageable pageable
    ) {
        log.debug("[Event] findWithFilters(q='{}', categoryId={}, dateFrom={}, dateTo={}, city='{}', page={}, size={})",
                q, categoryId, dateFrom, dateTo, city, pageable.getPageNumber(), pageable.getPageSize());

        Page<Event> events = eventRepository.findWithFilters(
                q, categoryId, dateFrom, dateTo, city, pageable
        );

        log.debug("[Event] findWithFilters -> {} eventos encontrados", events.getNumberOfElements());
        return events.map(this::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventSummaryResponseDto> findByOrganizer(String organizerEmail, boolean includeInactive, Pageable pageable) {
        log.debug("[Event] findByOrganizer(organizerEmail='{}', includeInactive={}, page={}, size={})",
                organizerEmail, includeInactive, pageable.getPageNumber(), pageable.getPageSize());

        User organizer = getUserOrThrow(organizerEmail);
        validateOrganizerRole(organizer);

        Page<Event> events = includeInactive
                ? eventRepository.findByOrganizerId(organizer.getId(), pageable)
                : eventRepository.findByOrganizerIdAndActiveTrue(organizer.getId(), pageable);

        log.debug("[Event] findByOrganizer -> {} eventos propios encontrados", events.getNumberOfElements());
        return events.map(this::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EventDetailResponseDto findById(Long id) {
        log.debug("[Event] Buscando evento id={}", id);
        Event event = getEventOrThrow(id);

        if ((!event.isActive() || event.getStatus() != EventStatus.PUBLISHED) && !isAuthorizedToViewUnpublished(event)) {
            log.warn("[Event] Intento no autorizado de consultar evento no publicado o inactivo id={}", id);
            throw new ResourceNotFoundException("No se encontró el evento con el id: " + id);
        }

        return toDetailDto(event);
    }

    @Override
    @Transactional
    public EventDetailResponseDto create(EventRequestDto request, String organizerEmail) {
        String title = request.title().trim();
        log.info("[Event] Creando evento '{}' para organizador='{}'", title, organizerEmail);

        User organizer = getUserOrThrow(organizerEmail);
        validateOrganizerRole(organizer);
        Category category = getCategoryOrThrow(request.categoryId());

        validateSchedule(request.date(), request.startTime(), request.endTime(), true);
        ensureTitleIsAvailable(title, organizer.getId(), null);


        Event event = Event.builder()
                .title(title)
                .description(request.description())
                .date(request.date())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .location(request.location())
                .capacity(request.capacity())
                .bannerUrl(request.bannerUrl() != null && !request.bannerUrl().isBlank() ? request.bannerUrl().trim() : null)
                .status(EventStatus.PENDING)
                .active(true)
                .category(category)
                .organizer(organizer)
                .build();

        eventRepository.save(event);
        log.info("[Event] Evento creado exitosamente id={} título='{}'", event.getId(), event.getTitle());

        return toDetailDto(event);
    }

    @Override
    @Transactional
    public EventDetailResponseDto update(Long id, EventRequestDto request, String userEmail) {
        log.info("[Event] Actualizando evento id={} por usuario='{}'", id, userEmail);

        Event event = getEventOrThrow(id);
        User user = getUserOrThrow(userEmail);
        validateOrganizerRole(user);
        validateOwnership(event, user);

        String newTitle = request.title().trim();
        ensureTitleIsAvailable(newTitle, event.getOrganizer().getId(), event);

        Category category = getCategoryOrThrow(request.categoryId());
        boolean requireFutureDate = event.isActive() || event.getStatus() == EventStatus.PUBLISHED;
        validateSchedule(request.date(), request.startTime(), request.endTime(), requireFutureDate);
        validateCapacityForEventUpdate(event, request.capacity());

        event.setTitle(newTitle);
        event.setDescription(request.description());
        event.setDate(request.date());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setLocation(request.location());
        event.setCapacity(request.capacity());
        event.setCategory(category);

        if (request.bannerUrl() != null) {
            String banner = request.bannerUrl().trim();
            event.setBannerUrl(banner.isEmpty() ? null : banner);
        }

        event = eventRepository.saveAndFlush(event);
        log.info("[Event] Evento actualizado id={} título='{}'", event.getId(), event.getTitle());

        return toDetailDto(event);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, EventStatus status, String userEmail) {
        log.info("[Event] Cambiando estado de evento id={} a status={} por usuario='{}'", id, status, userEmail);

        Event event = getEventOrThrow(id);
        User user = getUserOrThrow(userEmail);
        validateOrganizerRole(user);
        validateOwnership(event, user);
        validateStatusTransition(event.getStatus(), status);
        validateStatusChangePermission(user, status);

        if (status == EventStatus.PUBLISHED && event.getStatus() != EventStatus.PUBLISHED) {
            validatePublishable(event);
        }

        event.setStatus(status);
        eventRepository.save(event);

        log.info("[Event] Estado de evento actualizado id={} status={}", event.getId(), event.getStatus());
    }

    @Override
    @Transactional
    public void updateActive(Long id, boolean active, String userEmail) {
        log.info("[Event] Cambiando estado de activación id={} active={} por usuario='{}'", id, active, userEmail);

        Event event = getEventOrThrow(id);
        User user = getUserOrThrow(userEmail);
        validateOrganizerRole(user);
        validateOwnership(event, user);

        event.setActive(active);
        eventRepository.save(event);
        log.info("[Event] Visibilidad activa de evento actualizada id={} active={}", event.getId(), event.isActive());
    }

    // --------
    // Helpers
    // --------

    private Event getEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[Event] No se encontró el evento con id={}", id);
                    return new ResourceNotFoundException("No se encontró el evento con el id: " + id);
                });
    }

    private Category getCategoryOrThrow(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("[Event] Categoría id={} no encontrada", categoryId);
                    return new ResourceNotFoundException("No se encontró la categoría con el id: " + categoryId);
                });

        if (!category.isActive()) {
            log.warn("[Event] Rechazado: la categoría id={} '{}' está inactiva", categoryId, category.getName());
            throw new IllegalArgumentException("La categoría '" + category.getName() + "' está inactiva y no puede ser asignada a eventos.");
        }

        return category;
    }

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[Event] Usuario email='{}' no encontrado", email);
                    return new ResourceNotFoundException("No se encontró el usuario con el email: " + email);
                });
    }

    private boolean isAuthorizedToViewUnpublished(Event event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return false;
        }

        return userRepository.findByEmail(auth.getName())
                .map(u -> u.getRole() == UserRole.ADMIN || event.getOrganizer().getId().equals(u.getId()))
                .orElse(false);
    }

    private void validateOwnership(Event event, User user) {
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        boolean isOwner = event.getOrganizer().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            log.warn("[Event] Acceso denegado: usuario id={} no es dueño del evento id={}", user.getId(), event.getId());
            throw new AccessDeniedException("No tienes permisos para modificar este evento");
        }
    }

    private void validateOrganizerRole(User user) {
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.ORGANIZER) {
            log.warn("[Event] Acceso denegado: usuario id={} rol={} no puede gestionar eventos", user.getId(), user.getRole());
            throw new AccessDeniedException("Solo un organizador o administrador puede gestionar eventos");
        }
    }

    private void validateStatusChangePermission(User user, EventStatus status) {
        /*
        if (user.getRole() == UserRole.ORGANIZER && status == EventStatus.PUBLISHED) {
            log.warn("[Event] Organizador id={} intentó publicar un evento sin permisos de administrador", user.getId());
            throw new AccessDeniedException("Solo un administrador puede publicar eventos");
        }
        */
    }

    private void validatePublishable(Event event) {
        if (event.getDate().isBefore(LocalDate.now())) {
            log.warn("[Event] No se puede publicar el evento id={} con fecha pasada: date={}", event.getId(), event.getDate());
            throw new IllegalArgumentException("No se puede publicar un evento cuya fecha ya pasó");
        }
    }

    private void validateCapacityForEventUpdate(Event event, int newCapacity) {
        int allocatedCapacity = ticketTypeRepository.sumCapacityByEventId(event.getId());
        if (newCapacity < allocatedCapacity) {
            log.warn("[Event] Capacidad inválida: evento id={}, nueva={}, asignada a tarifas={}",
                    event.getId(), newCapacity, allocatedCapacity);
            throw new IllegalArgumentException(
                    "La capacidad del evento no puede ser menor a la capacidad asignada a las tarifas (" + allocatedCapacity + ")"
            );
        }
    }

    private void validateSchedule(LocalDate date, LocalTime startTime, LocalTime endTime, boolean requireFutureDate) {
        if (requireFutureDate && date != null && date.isBefore(LocalDate.now())) {
            log.warn("[Event] Fecha inválida en el pasado: date={}", date);
            throw new IllegalArgumentException("La fecha del evento no puede ser anterior a la fecha actual");
        }
        if (!endTime.isAfter(startTime)) {
            log.warn("[Event] Horario inválido: startTime={}, endTime={}", startTime, endTime);
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }
    }

    private void validateStatusTransition(EventStatus currentStatus, EventStatus targetStatus) {
        if (currentStatus == null || targetStatus == null || currentStatus == targetStatus) {
            return;
        }
        if (currentStatus == EventStatus.CANCELLED || currentStatus == EventStatus.FINISHED) {
            log.warn("[Event] Transición de estado inválida: {} -> {}", currentStatus, targetStatus);
            throw new IllegalStateException("No se puede cambiar el estado de un evento que ya está " + currentStatus);
        }
    }

    private void ensureTitleIsAvailable(String title, Long organizerId, Event currentEvent) {
        if (currentEvent != null && currentEvent.getTitle().equalsIgnoreCase(title)) {
            return;
        }

        if (eventRepository.existsByTitleIgnoreCaseAndOrganizerId(title, organizerId)) {
            log.warn("[Event] Título duplicado para organizador id={}: '{}'", organizerId, title);
            throw new DuplicateResourceException("Ya tienes un evento registrado con el título: " + title);
        }
    }

    private EventSummaryResponseDto toSummaryDto(Event event) {
        return new EventSummaryResponseDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getLocation(),
                event.getCapacity(),
                event.getBannerUrl(),
                event.getCategory() != null ? event.getCategory().getName() : null,
                event.getStatus(),
                event.isActive()
        );
    }

    private EventDetailResponseDto toDetailDto(Event event) {
        return new EventDetailResponseDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getLocation(),
                event.getCapacity(),
                event.getBannerUrl(),
                event.getStatus(),
                event.isActive(),
                event.getCategory() != null ? event.getCategory().getId() : null,
                event.getCategory() != null ? event.getCategory().getName() : null,
                event.getOrganizer() != null ? event.getOrganizer().getId() : null,
                event.getOrganizer() != null ? (event.getOrganizer().getFirstName() + " " + event.getOrganizer().getLastName()) : null,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}