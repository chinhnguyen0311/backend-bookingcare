package com.IMIC.booking_care.appointment.controller;

import com.IMIC.booking_care.appointment.dto.request.AppointmentRequest;
import com.IMIC.booking_care.appointment.dto.request.MedicalScheduleSlotRequest;
import com.IMIC.booking_care.appointment.dto.response.AppointmentResponse;
import com.IMIC.booking_care.appointment.dto.response.MedicalScheduleSlotResponse;
import com.IMIC.booking_care.appointment.service.MedicalScheduleService;
import com.IMIC.booking_care.common.dto.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/booking")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MedicalScheduleController {
    MedicalScheduleService medicalScheduleService;
    @GetMapping("/slots")
    public ApiResponse<List<MedicalScheduleSlotResponse>> getSlotsByDoctorAndDate(
            @RequestParam UUID doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate slotDate
    ) {
        MedicalScheduleSlotRequest request = MedicalScheduleSlotRequest.builder()
                .doctorId(doctorId)
                .slotDate(slotDate)
                .build();

        List<MedicalScheduleSlotResponse> response = medicalScheduleService.getMedicalScheduleSlot(request);

        return ApiResponse.<List<MedicalScheduleSlotResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách slot thành công")
                .data(response)
                .build();
    }
    @GetMapping("/working-dates")
    public ApiResponse<List<LocalDate>> getWorkingDates(@RequestParam UUID doctorId) {
        List<LocalDate> dates = medicalScheduleService.getWorkingDates(doctorId);

        String message = dates.isEmpty()
                ? "Bác sĩ hiện không có lịch làm việc"
                : "Lấy danh sách ngày làm việc thành công";

        return ApiResponse.<List<LocalDate>>builder()
                .success(true)
                .code(200)
                .message(message)
                .data(dates)
                .build();
    }
    @PostMapping("/book")
    public ApiResponse<AppointmentResponse> bookAppointment(@RequestBody AppointmentRequest request) {
        return ApiResponse.<AppointmentResponse>builder()
                .success(true)
                .code(200)
                .message("Đặt lịch khám thành công")
                .data(medicalScheduleService.bookAppointment(request))
                .build();
    }
}
