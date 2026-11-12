package com.backend.eventia.controller;

import com.backend.eventia.dto.user.ChangePasswordRequestDto;
import com.backend.eventia.dto.user.DeactivateAccountRequestDto;
import com.backend.eventia.dto.user.OrganizerCustomerResponseDto;
import com.backend.eventia.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Page<OrganizerCustomerResponseDto>> findCustomers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        log.debug("[UserController] GET /api/users - organizador='{}' q='{}' active={} page={} size={}",
                authentication.getName(), q, active, page, size);
        return ResponseEntity.ok(userService.findCustomers(authentication.getName(), q, active, page, size));
    }

    @PatchMapping("/me/password")
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER')")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequestDto request
    ) {
        log.info("[UserController] PATCH /api/users/me/password - usuario='{}'", authentication.getName());
        userService.changePassword(authentication.getName(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/deactivate")
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZER')")
    public ResponseEntity<Void> deactivateAccount(
            Authentication authentication,
            @Valid @RequestBody DeactivateAccountRequestDto request
    ) {
        log.info("[UserController] PATCH /api/users/me/deactivate - usuario='{}'", authentication.getName());
        userService.deactivateAccount(authentication.getName(), request.password());
        return ResponseEntity.noContent().build();
    }
}
