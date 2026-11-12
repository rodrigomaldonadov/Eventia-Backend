package com.backend.eventia.service.impl;

import com.backend.eventia.dto.user.OrganizerCustomerResponseDto;
import com.backend.eventia.exception.ResourceNotFoundException;
import com.backend.eventia.model.Event;
import com.backend.eventia.model.User;
import com.backend.eventia.model.enums.UserRole;
import com.backend.eventia.repository.EventRepository;
import com.backend.eventia.repository.OrderRepository;
import com.backend.eventia.repository.UserRepository;
import com.backend.eventia.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void deactivateAccount(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario con el correo: " + email));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("[User] Desactivación rechazada: contraseña incorrecta para id={}", user.getId());
            throw new IllegalArgumentException("La contraseña es incorrecta");
        }

        user.setActive(false);
        user.setDeactivatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("[User] Cuenta desactivada id={} email='{}'", user.getId(), user.getEmail());

        if (user.getRole() == UserRole.ORGANIZER) {
            List<Event> activeEvents = eventRepository.findByOrganizerIdAndActiveTrue(user.getId());
            activeEvents.forEach(event -> event.setActive(false));
            eventRepository.saveAll(activeEvents);
            log.info("[User] Eventos ocultados del catálogo para organizador id={}: {}",
                    user.getId(), activeEvents.size());
        }
    }

    @Override
    @Transactional
    public void changePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario con el correo: " + email));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("[User] Contraseña actualizada id={} email='{}'", user.getId(), user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizerCustomerResponseDto> findCustomers(String organizerEmail, String q, Boolean active,
                                                            int page, int size) {
        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario con el correo: " + organizerEmail));

        if (organizer.getRole() != UserRole.ORGANIZER) {
            throw new AccessDeniedException("Solo un organizador puede consultar sus compradores");
        }

        String search = q == null || q.isBlank() ? null : q.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        log.debug("[User] Buscando compradores de organizador id={} (q='{}', active={}, page={}, size={})",
                organizer.getId(), search, active, safePage, safeSize);

        return orderRepository.findCustomersByOrganizer(
                organizer.getId(), search, active, PageRequest.of(safePage, safeSize));
    }

    @Override
    @Transactional
    public User reactivateUser(User user) {
        user.setActive(true);
        user.setDeactivatedAt(null);
        User saved = userRepository.save(user);
        log.info("[User] Cuenta reactivada id={} email='{}'", saved.getId(), saved.getEmail());
        return saved;
    }
}
