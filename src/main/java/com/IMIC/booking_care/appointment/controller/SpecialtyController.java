package com.IMIC.booking_care.appointment.controller;

import com.IMIC.booking_care.appointment.dto.response.SpecialtyResponse;
import com.IMIC.booking_care.appointment.service.SpecialtyService;
import com.IMIC.booking_care.common.dto.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpecialtyController {
    SpecialtyService specialtyService;

    @GetMapping
    public ApiResponse<List<SpecialtyResponse>> getAllSpecialties() {
        return ApiResponse.<List<SpecialtyResponse>>builder()
                .success(true)
                .code(200)
                .message("Lấy danh sách khoa thành công")
                .data(specialtyService.getAllSpecialties())
                .build();
    }
}
