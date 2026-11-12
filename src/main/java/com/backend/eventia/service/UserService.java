package com.backend.eventia.service;

import com.backend.eventia.dto.auth.UserResponseDto;
import com.backend.eventia.dto.auth.UserUpdateRequestDto;
import com.backend.eventia.dto.auth.UserPasswordChangeDto;

/**
 * Servicio para la gestión del perfil del usuario autenticado.
 * Operaciones sobre el propio usuario (me endpoints).
 */
public interface UserService {

    /**
     * Obtiene el perfil completo del usuario autenticado.
     */
    UserResponseDto getCurrentUserProfile(String email);

    /**
     * Actualiza los datos del perfil del usuario autenticado.
     */
    UserResponseDto updateProfile(String email, UserUpdateRequestDto request);

    /**
     * Cambia la contraseña del usuario autenticado.
     * Valida que la contraseña actual coincida antes de aplicar el cambio.
     */
    void changePassword(String email, UserPasswordChangeDto request);
}
