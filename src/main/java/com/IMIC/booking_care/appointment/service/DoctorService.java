package com.IMIC.booking_care.appointment.service;

import com.IMIC.booking_care.appointment.dto.response.DoctorResponse;
import com.IMIC.booking_care.appointment.entity.Doctor;
import com.IMIC.booking_care.appointment.repository.DoctorRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DoctorService {
    DoctorRepository doctorRepository;
    public List<DoctorResponse> getAllDoctors() {
        log.info("Fetching all doctors");

        List<Object[]> results = doctorRepository.findAllDoctor();

        if (results.isEmpty()) {
            log.warn("Không tìm thấy bác sĩ");
            return Collections.emptyList();
        }

        return results.stream()
                .map(row -> DoctorResponse.builder()
                        .doctorId((UUID) row[0])
                        .userId((UUID) row[1])
                        .fullName((String) row[2])
                        .specialty((String) row[3])
                        .bio((String) row[4])
                        .experience((String) row[5])
                        .treatmentScope((String) row[6])
                        .build())
                .toList();
    }
    public List<DoctorResponse> getDoctorsBySpecialty(UUID specialtyId) {
        log.info("Fetching doctors for specialtyId={}", specialtyId);

        List<Object[]> results = doctorRepository.findAllBySpecialtyId(specialtyId);

        if (results.isEmpty()) {
            log.warn("Không tìm thấy bác sĩ thuộc khoa specialtyId={}", specialtyId);
            return Collections.emptyList();
        }

        return results.stream()
                .map(row -> DoctorResponse.builder()
                        .doctorId((UUID) row[0])
                        .userId((UUID) row[1])
                        .fullName((String) row[2])
                        .specialty((String) row[3])
                        .bio((String) row[4])
                        .experience((String) row[5])
                        .treatmentScope((String) row[6])
                        .build())
                .toList();
    }
}
