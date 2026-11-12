package com.backend.eventia.controller;

import com.backend.eventia.dto.auth.UserResponseDto;
import com.backend.eventia.dto.auth.UserUpdateRequestDto;
import com.backend.eventia.dto.auth.UserPasswordChangeDto;
import com.backend.eventia.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Obtiene el perfil del usuario autenticado.
     * Endpoint: GET /api/users/me (complemento de GET /auth/me con datos extendidos)
     */
    @GetMapping
    public ResponseEntity<UserResponseDto> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        log.debug("[UserController] GET /api/users/me - usuario='{}'", email);
        UserResponseDto profile = userService.getCurrentUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    /**
     * Actualiza los datos del perfil del usuario autenticado.
     * Endpoint: PATCH /api/users/me
     */
    @PatchMapping
    public ResponseEntity<UserResponseDto> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequestDto request
    ) {
        String email = authentication.getName();
        log.info("[UserController] PATCH /api/users/me - actualizando perfil usuario='{}'", email);
        UserResponseDto updated = userService.updateProfile(email, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     * Endpoint: PUT /api/users/me/password
     */
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody UserPasswordChangeDto request
    ) {
        String email = authentication.getName();
        log.info("[UserController] PUT /api/users/me/password - cambiando contraseña usuario='{}'", email);
        userService.changePassword(email, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Desactiva lógicamente la cuenta del usuario autenticado (baja lógica).
     * Endpoint: PATCH /api/users/me/deactivate
     */
    @PatchMapping("/deactivate")
    public ResponseEntity<Void> deactivateAccount(Authentication authentication) {
        String email = authentication.getName();
        log.info("[UserController] PATCH /api/users/me/deactivate - desactivando usuario='{}'", email);
        userService.deactivateAccount(email);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
}
