package com.IMIC.booking_care.appointment.service;

import com.IMIC.booking_care.appointment.dto.response.SpecialtyResponse;
import com.IMIC.booking_care.appointment.repository.SpecialtyRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SpecialtyService {

    SpecialtyRepository specialtyRepository;

    public List<SpecialtyResponse> getAllSpecialties() {
        log.info("Fetching all specialties");

        return specialtyRepository.findAll()
                .stream()
                .map(specialty -> SpecialtyResponse.builder()
                        .specialtyId(specialty.getSpecialtyId())
                        .name(specialty.getName())
                        .description(specialty.getDescription())
                        .build())
                .toList();
    }
}
