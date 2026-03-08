package com.IMIC.booking_care.authentication.controller;

import com.IMIC.booking_care.authentication.dto.request.AuthRequest;
import com.IMIC.booking_care.authentication.dto.request.ChangePasswordRequest;
import com.IMIC.booking_care.authentication.dto.request.CreateUserRequest;
import com.IMIC.booking_care.authentication.dto.request.LogoutRequest;
import com.IMIC.booking_care.authentication.dto.request.RegisterRequest;
import com.IMIC.booking_care.authentication.dto.response.AuthResponse;
import com.IMIC.booking_care.authentication.service.AuthService;
import com.IMIC.booking_care.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .code(200)
                .message("Đăng ký thành công")
                .data(authService.register(request))
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .code(200)
                .message("Đăng nhập thành công")
                .data(authService.login(request))
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestBody(required = false) LogoutRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        authService.logout(request != null ? request : LogoutRequest.builder().build(), authorization);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Đăng xuất thành công")
                .data(null)
                .build();
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        authService.changePassword(request, authorization);
        return ApiResponse.<Void>builder()
                .success(true)
                .code(200)
                .message("Đổi mật khẩu thành công")
                .data(null)
                .build();
    }

    /**
     * Tạo tài khoản (Bác sĩ hoặc Admin). Chỉ Admin đã đăng nhập mới gọi được.
     * Bác sĩ/Admin sau khi được tạo sẽ dùng chung endpoint Login/Logout như bệnh nhân.
     */
    @PostMapping("/admin/users")
    public ApiResponse<AuthResponse> createUserByAdmin(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader("Authorization") String authorization
    ) {
        return ApiResponse.<AuthResponse>builder()
                .success(true)
                .code(200)
                .message("Tạo tài khoản thành công")
                .data(authService.createUserByAdmin(request, authorization))
                .build();
    }
}
