package com.backend.eventia.service;

import com.backend.eventia.dto.auth.AuthResponseDto;
import com.backend.eventia.dto.auth.AuthUserResponseDto;
import com.backend.eventia.dto.auth.LoginRequestDto;
import com.backend.eventia.dto.auth.RegisterRequestDto;
import com.backend.eventia.dto.auth.ReactivateRequestDto;

public interface AuthService {
    AuthResponseDto login(LoginRequestDto request);
    AuthResponseDto register(RegisterRequestDto request);
    AuthResponseDto reactivate(ReactivateRequestDto request);
    AuthUserResponseDto getCurrentUser(String email);
}
