package com.backend.eventia.service.impl;

import com.backend.eventia.dto.auth.AuthResponseDto;
import com.backend.eventia.dto.auth.AuthUserResponseDto;
import com.backend.eventia.dto.auth.AuthUserSummaryDto;
import com.backend.eventia.dto.auth.ForgotPasswordRequestDto;
import com.backend.eventia.dto.auth.ForgotPasswordResponseDto;
import com.backend.eventia.dto.auth.LoginRequestDto;
import com.backend.eventia.dto.auth.RegisterRequestDto;
import com.backend.eventia.dto.auth.ReactivateRequestDto;
import com.backend.eventia.dto.auth.ResetPasswordRequestDto;
import com.backend.eventia.exception.DuplicateResourceException;
import com.backend.eventia.exception.ResourceNotFoundException;
import com.backend.eventia.model.User;
import com.backend.eventia.model.enums.UserRole;
import com.backend.eventia.repository.UserRepository;
import com.backend.eventia.security.JwtUtils;
import com.backend.eventia.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int RESET_CODE_TTL_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /** Códigos de recuperación en memoria: email -> código con vencimiento. */
    private final Map<String, ResetCode> resetCodes = new ConcurrentHashMap<>();

    private record ResetCode(String code, LocalDateTime expiresAt) {
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {

        String email = request.email().trim().toLowerCase();
        log.info("[Auth] Recibida petición de LOGIN para: {}", email);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro el usuario autenticado con el correo: " + email));

        String token = jwtUtils.generateToken(user);

        log.info("[Auth] LOGIN exitoso para: {}", email);
        return buildAuthResponse(token, user);
    }

    @Override
    @Transactional
    public AuthResponseDto reactivate(ReactivateRequestDto request) {
        String email = request.email().trim().toLowerCase();
        String dni = request.dni().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Datos de reactivación inválidos"));

        if (user.isActive() || !user.getDni().equals(dni)) {
            throw new BadCredentialsException("Datos de reactivación inválidos");
        }

        user.setActive(true);
        user.setDeactivatedAt(null);
        User reactivated = userRepository.save(user);
        String token = jwtUtils.generateToken(reactivated);
        log.info("[Auth] Cuenta reactivada para: {}", email);
        return buildAuthResponse(token, reactivated);
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {

        String email = request.email().trim().toLowerCase();
        String documento = request.dni().trim();
        UserRole role = mapRole(request.role());
        log.info("[Auth] Recibida petición de REGISTRO para: {} (rol={})", email, role);

        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.isActive()) {
                log.warn("[Auth] REGISTRO rechazado: el email pertenece a una cuenta inactiva -> {}", email);
                throw new DuplicateResourceException(
                        "El correo ya está registrado en una cuenta inactiva. Puedes reactivar tu cuenta.");
            }
            log.warn("[Auth] REGISTRO rechazado: el email ya existe -> {}", email);
            throw new DuplicateResourceException("El correo ya está registrado.");
        });

        if (userRepository.existsByDni(documento)) {
            log.warn("[Auth] REGISTRO rechazado: el documento ya existe -> {}", documento);
            throw new DuplicateResourceException("El número de documento ya está registrado.");
        }

        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .documentType(request.documentType())
                .dni(documento)
                .email(email)
                .cellphone(request.cellphone().trim())
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .build();
        userRepository.save(user);

        String token = jwtUtils.generateToken(user);
        log.info("[Auth] REGISTRO exitoso para: {} (id={})", email, user.getId());
        return buildAuthResponse(token, user);
    }

    private UserRole mapRole(String role) {
        return switch (role.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "CLIENTE" -> UserRole.USER;
            case "ORGANIZADOR" -> UserRole.ORGANIZER;
            default -> throw new IllegalArgumentException("El rol debe ser CLIENTE u ORGANIZADOR");
        };
    }

    private AuthResponseDto buildAuthResponse(String token, User user) {
        AuthUserSummaryDto userDto = new AuthUserSummaryDto(
                user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.getDocumentType(),
                user.getDni(),
                user.getCellphone()
        );

        return new AuthResponseDto(
                token,
                "Bearer",
                jwtUtils.getExpirationInSeconds(),
                userDto
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserResponseDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontro el usuario autenticado con el correo: " + email));

        log.debug("[Auth] Perfil del usuario autenticado consultado: id={} email={}", user.getId(), user.getEmail());

        return new AuthUserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.getDocumentType(),
                user.getDni(),
                user.getCellphone()
        );
    }

    @Override
    public ForgotPasswordResponseDto requestPasswordReset(ForgotPasswordRequestDto request) {
        String email = request.email().trim().toLowerCase();

        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una cuenta registrada con el correo: " + email));

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        resetCodes.put(email, new ResetCode(code, LocalDateTime.now().plusMinutes(RESET_CODE_TTL_MINUTES)));

        log.info("[Auth] Código de recuperación generado para: {} (vence en {} min)", email, RESET_CODE_TTL_MINUTES);

        return new ForgotPasswordResponseDto(
                email,
                code,
                RESET_CODE_TTL_MINUTES,
                "Código de recuperación generado. Vence en " + RESET_CODE_TTL_MINUTES + " minutos."
        );
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        String email = request.email().trim().toLowerCase();
        String code = request.code().trim();

        ResetCode stored = resetCodes.get(email);
        boolean expired = stored == null || stored.expiresAt().isBefore(LocalDateTime.now());
        boolean wrongCode = stored != null && !stored.code().equals(code);

        if (expired || wrongCode) {
            resetCodes.remove(email);
            throw new IllegalArgumentException("El código de recuperación es inválido o ha vencido.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una cuenta registrada con el correo: " + email));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        resetCodes.remove(email);

        log.info("[Auth] Contraseña restablecida para: {} (id={})", email, user.getId());
    }
}
