package com.IMIC.booking_care.authentication.controller;

import com.IMIC.booking_care.authentication.dto.request.AuthRequest;
import com.IMIC.booking_care.authentication.dto.response.AuthResponse;
import com.IMIC.booking_care.authentication.service.AuthService;
import com.IMIC.booking_care.common.dto.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    AuthService authService;
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody AuthRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .code(200)
                .message("Đăng nhập thành công")
                .data(authService.login(request))
                .build();
    }
}
