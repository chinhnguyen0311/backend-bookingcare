package com.IMIC.booking_care.appointment.controller;

import com.IMIC.booking_care.appointment.dto.response.DoctorResponse;
import com.IMIC.booking_care.appointment.service.DoctorService;
import com.IMIC.booking_care.common.dto.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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

}
