package com.IMIC.booking_care.user.controller;

import com.IMIC.booking_care.appointment.enums.AppointmentStatus;
import com.IMIC.booking_care.common.dto.ApiResponse;
import com.IMIC.booking_care.user.dto.response.PatientAppointmentResponse;
import com.IMIC.booking_care.user.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @GetMapping("/appointments")
    public ApiResponse<List<PatientAppointmentResponse>> getAppointmentsByStatus(
            @RequestHeader("Authorization") String token,
            @RequestParam AppointmentStatus status
    ) {
        List<PatientAppointmentResponse> response = userService.getAppointmentsByStatus(token, status);

        String message = switch (status) {
            case PENDING -> response.isEmpty() ? "Không có lịch hẹn nào đang chờ xác nhận" : "Lấy danh sách lịch hẹn PENDING thành công";
            case CONFIRMED -> response.isEmpty() ? "Không có lịch hẹn nào đã xác nhận" : "Lấy danh sách lịch hẹn CONFIRMED thành công";
            case CANCELLED -> response.isEmpty() ? "Không có lịch hẹn nào đã bị huỷ" : "Lấy danh sách lịch hẹn CANCELLED thành công";
            case COMPLETED -> response.isEmpty() ? "Không có lịch hẹn nào đã hoàn thành" : "Lấy danh sách lịch hẹn COMPLETED thành công";
            default -> "Lấy danh sách lịch hẹn thành công";
        };

        return ApiResponse.<List<PatientAppointmentResponse>>builder()
                .success(true)
                .code(200)
                .message(message)
                .data(response)
                .build();
    }
    @GetMapping("/appointments/upcoming")
    public ApiResponse<List<PatientAppointmentResponse>> getUpcomingAppointments(
            @RequestHeader("Authorization") String token
    ) {
        List<PatientAppointmentResponse> response = userService.getUpcomingAppointments(token);

        String message = response.isEmpty()
                ? "Không có lịch khám sắp tới"
                : "Lấy danh sách lịch khám sắp tới thành công";

        return ApiResponse.<List<PatientAppointmentResponse>>builder()
                .success(true)
                .code(200)
                .message(message)
                .data(response)
                .build();
    }
}
