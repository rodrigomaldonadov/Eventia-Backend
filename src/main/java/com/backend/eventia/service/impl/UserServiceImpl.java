package com.backend.eventia.service.impl;

import com.backend.eventia.dto.auth.UserResponseDto;
import com.backend.eventia.dto.auth.UserUpdateRequestDto;
import com.backend.eventia.dto.auth.UserPasswordChangeDto;
import com.backend.eventia.exception.ResourceNotFoundException;
import com.backend.eventia.exception.DuplicateResourceException;
import com.backend.eventia.model.User;
import com.backend.eventia.repository.UserRepository;
import com.backend.eventia.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario con el correo: " + email));

        return mapToDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateProfile(String email, UserUpdateRequestDto request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario con el correo: " + email));

        // Validar que el nuevo email no esté en uso por otro usuario
        if (!user.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), user.getId())) {
            throw new DuplicateResourceException("El correo electrónico ya está en uso por otro usuario");
        }

        // Validar que el nuevo DNI no esté en uso por otro usuario
        if (!user.getDni().equals(request.dni())
                && userRepository.existsByDniAndIdNot(request.dni(), user.getId())) {
            throw new DuplicateResourceException("El DNI ya está registrado por otro usuario");
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setDni(request.dni());
        user.setCellphone(request.cellphone());

        user = userRepository.saveAndFlush(user);
        log.info("[UserService] Perfil actualizado para usuario id={} email={}", user.getId(), user.getEmail());

        return mapToDto(user);
    }

    @Override
    @Transactional
    public void changePassword(String email, UserPasswordChangeDto request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario con el correo: " + email));

        // Validar que la contraseña actual coincida
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            log.warn("[UserService] Contraseña actual incorrecta para usuario id={}", user.getId());
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        // Validar que la nueva contraseña sea diferente de la actual
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña no puede ser igual a la actual");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("[UserService] Contraseña cambiada exitosamente para usuario id={}", user.getId());
    }

    @Override
    @Transactional
    public void deactivateAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario con el correo: " + email));

        if (!user.isActive()) {
            log.warn("[UserService] Usuario id={} ya está inactivo", user.getId());
            return;
        }

        user.setActive(false);
        userRepository.save(user);
        log.info("[UserService] Cuenta desactivada lógicamente para usuario id={} email={}", user.getId(), user.getEmail());
    }

    private UserResponseDto mapToDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getDni(),
                user.getCellphone(),
                user.getRole().name(),
                user.isActive()
        );
    }
}
