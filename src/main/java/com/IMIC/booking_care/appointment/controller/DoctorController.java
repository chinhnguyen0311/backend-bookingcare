package com.IMIC.booking_care.appointment.controller;

import com.IMIC.booking_care.appointment.dto.response.AppointmentResponse;
import com.IMIC.booking_care.appointment.dto.response.DoctorResponse;
import com.IMIC.booking_care.appointment.dto.response.TodayAppointmentResponse;
import com.IMIC.booking_care.appointment.enums.AppointmentStatus;
import com.IMIC.booking_care.appointment.service.DoctorService;
import com.IMIC.booking_care.common.dto.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.IMIC.booking_care.appointment.enums.AppointmentStatus.CONFIRMED;
import static com.IMIC.booking_care.appointment.enums.AppointmentStatus.PENDING;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorController {
    DoctorService doctorService;

    @GetMapping
    public ApiResponse<List<DoctorResponse>> getAllDoctors() {
        return ApiResponse.<List<DoctorResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách bác sĩ thành công")
                .data(doctorService.getAllDoctors())
                .build();
    }
    @GetMapping("/specialty/{specialtyId}")
    public ApiResponse<List<DoctorResponse>> getDoctorsBySpecialty(
            @PathVariable UUID specialtyId
    ) {
        return ApiResponse.<List<DoctorResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách bác sĩ theo khoa thành công")
                .data(doctorService.getDoctorsBySpecialty(specialtyId))
                .build();
    }
    @GetMapping("/appointment-today")
    public ApiResponse<List<TodayAppointmentResponse>> getTodayAppointments(
            @RequestHeader("Authorization") String token
    ) {
        List<TodayAppointmentResponse> response = doctorService.getTodayAppointments(token);

        String message = response.isEmpty()
                ? "Hôm nay không có lịch khám nào"
                : "Lấy danh sách lịch khám hôm nay thành công";

        return ApiResponse.<List<TodayAppointmentResponse>>builder()
                .success(true)
                .code(200)
                .message(message)
                .data(response)
                .build();
    }
    @GetMapping("/appointment-status")
    public ApiResponse<List<TodayAppointmentResponse>> getAppointmentsByStatus(
            @RequestHeader("Authorization") String token,
            @RequestParam AppointmentStatus status
    ) {
        List<TodayAppointmentResponse> response = doctorService.getAppointmentsByStatus(token, status);

        String message = switch (status) {
            case PENDING -> response.isEmpty() ? "Không có lịch khám nào đang chờ xác nhận" : "Lấy danh sách lịch khám PENDING thành công";
            case CONFIRMED -> response.isEmpty() ? "Không có lịch khám nào đã xác nhận" : "Lấy danh sách lịch khám CONFIRMED thành công";
            case CANCELLED -> response.isEmpty() ? "Không có lịch khám nào đã bị huỷ" : "Lấy danh sách lịch khám CANCELLED thành công";
            default -> "Lấy danh sách lịch khám thành công";
        };

        return ApiResponse.<List<TodayAppointmentResponse>>builder()
                .success(true)
                .code(200)
                .message(message)
                .data(response)
                .build();
    }
    @PatchMapping("/appointment/{appointmentId}/confirm")
    public ApiResponse<AppointmentResponse> confirmAppointment(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID appointmentId
    ) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .code(200)
                .message("Xác nhận lịch hẹn thành công")
                .data(doctorService.confirmAppointment(token, appointmentId))
                .build();
    }
    @PatchMapping("/appointment/{appointmentId}/cancel")
    public ApiResponse<AppointmentResponse> cancelAppointment(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID appointmentId
    ) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .code(200)
                .message("Huỷ lịch hẹn thành công")
                .data(doctorService.cancelAppointment(token, appointmentId))
                .build();
    }
    @GetMapping("/examining")
    public ApiResponse<TodayAppointmentResponse> getNextAppointment(
            @RequestHeader("Authorization") String token
    ) {
        TodayAppointmentResponse response = doctorService.getNextAppointment(token);

        String message = response == null
                ? "Hôm nay không còn ca khám nào"
                : "Lấy ca khám tiếp theo thành công";

        return ApiResponse.<TodayAppointmentResponse>builder()
                .success(true)
                .code(200)
                .message(message)
                .data(response)
                .build();
    }
    @PatchMapping("/appointment/{appointmentId}/complete")
    public ApiResponse<AppointmentResponse> completeAppointment(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID appointmentId
    ) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .code(200)
                .message("Hoàn thành lịch hẹn thành công")
                .data(doctorService.completeAppointment(token, appointmentId))
                .build();
    }
}
