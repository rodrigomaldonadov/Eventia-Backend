package com.backend.eventia.controller;

import com.backend.eventia.dto.auth.AuthResponseDto;
import com.backend.eventia.dto.auth.AuthUserResponseDto;
import com.backend.eventia.dto.auth.LoginRequestDto;
import com.backend.eventia.dto.auth.RegisterRequestDto;
import com.backend.eventia.dto.auth.ReactivateRequestDto;
import com.backend.eventia.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("[AuthController] POST /auth/register recibido (email={})", request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request,
                                                 HttpServletRequest httpRequest) {
        log.info("[AuthController] POST /auth/login recibido (email={})", request.email());
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (DisabledException ex) {
            log.warn("[Auth] Login fallido: cuenta desactivada | email='{}' ip='{}'",
                    request.email(), getClientIp(httpRequest));
            throw ex;
        } catch (BadCredentialsException ex) {
            log.warn("[Auth] Login fallido: credenciales invalidas | email='{}' ip='{}'",
                    request.email(), getClientIp(httpRequest));
            throw ex;
        }

    }

    @PostMapping("/reactivate")
    public ResponseEntity<AuthResponseDto> reactivate(@Valid @RequestBody ReactivateRequestDto request) {
        return ResponseEntity.ok(authService.reactivate(request));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    // GET /auth/me -> perfil del usuario autenticado (identificado por el JWT)
    @GetMapping("/me")
    public ResponseEntity<AuthUserResponseDto> me(Authentication authentication) {
        String email = authentication.getName();
        log.info("[AuthController] GET /auth/me - usuario autenticado identificado mediante Authentication: {}", email);
        return ResponseEntity.ok(authService.getCurrentUser(email));
    }

}
