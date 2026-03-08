package com.IMIC.booking_care.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(9999, "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    // Auth
    USER_NOT_FOUND(1001, "Tài khoản không tồn tại", HttpStatus.NOT_FOUND),
    INVALID_PASSWORD(1002, "Mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    USERNAME_EXISTS(1003, "Tên đăng nhập đã được sử dụng", HttpStatus.CONFLICT),
    EMAIL_EXISTS(1004, "Email đã được đăng ký", HttpStatus.CONFLICT),
    INVALID_OLD_PASSWORD(1005, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1006, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(1007, "Refresh token không hợp lệ hoặc đã bị thu hồi", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1008, "Tài khoản không có quyền truy cập", HttpStatus.FORBIDDEN);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
