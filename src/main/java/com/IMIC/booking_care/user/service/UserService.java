package com.IMIC.booking_care.user.service;

import com.IMIC.booking_care.appointment.entity.Appointment;
import com.IMIC.booking_care.appointment.entity.Doctor;
import com.IMIC.booking_care.appointment.entity.DoctorAvailableSlot;
import com.IMIC.booking_care.appointment.enums.AppointmentStatus;
import com.IMIC.booking_care.appointment.repository.AppointmentRepository;
import com.IMIC.booking_care.appointment.repository.DoctorAvailableSlotRepository;
import com.IMIC.booking_care.appointment.repository.DoctorRepository;
import com.IMIC.booking_care.user.dto.response.PatientAppointmentResponse;
import com.IMIC.booking_care.user.entity.User;
import com.IMIC.booking_care.user.repository.UserRepository;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    DoctorRepository doctorRepository;
    UserRepository userRepository;
    AppointmentRepository appointmentRepository;
    DoctorAvailableSlotRepository doctorAvailableSlotRepository;
    public List<PatientAppointmentResponse> getAppointmentsByStatus(String token, AppointmentStatus status) {
        UUID userId = extractUserIdFromToken(token);
        log.info("Fetching {} appointments for userId={}", status, userId);

        List<Appointment> appointments = appointmentRepository
                .findAppointmentsByUserIdAndStatus(userId, status);

        if (appointments.isEmpty()) {
            log.info("Không có lịch hẹn {} nào cho userId={}", status, userId);
            return Collections.emptyList();
        }

        // Lấy tất cả slot 1 lần
        Map<UUID, DoctorAvailableSlot> slotMap = doctorAvailableSlotRepository
                .findAllById(appointments.stream().map(Appointment::getSlotId).toList())
                .stream()
                .collect(Collectors.toMap(DoctorAvailableSlot::getSlotId, s -> s));

        // Lấy tất cả doctor 1 lần
        Map<UUID, Doctor> doctorMap = doctorRepository
                .findAllById(appointments.stream().map(Appointment::getDoctorId).toList())
                .stream()
                .collect(Collectors.toMap(Doctor::getDoctorId, d -> d));

        return appointments.stream()
                .map(appointment -> {
                    DoctorAvailableSlot slot = slotMap.get(appointment.getSlotId());
                    Doctor doctor = doctorMap.get(appointment.getDoctorId());

                    // Lấy tên bác sĩ từ bảng users
                    String doctorName = "N/A";
                    String specialty = "N/A";
                    if (doctor != null) {
                        User doctorUser = userRepository.findById(doctor.getUserId()).orElse(null);
                        doctorName = doctorUser != null ? doctorUser.getFullName() : "N/A";

                    }

                    return PatientAppointmentResponse.builder()
                            .appointmentId(appointment.getAppointmentId())
                            .doctorName(doctorName)
                            .specialty(specialty)
                            .appointmentDate(appointment.getAppointmentDate())
                            .startTime(slot != null ? slot.getStartTime() : null)
                            .endTime(slot != null ? slot.getEndTime() : null)
                            .expectedTime(appointment.getExpectedTime())
                            .status(appointment.getStatus())
                            .notes(appointment.getNotes())
                            .createdAt(appointment.getCreatedAt())
                            .build();
                })
                .toList();
    }
    public List<PatientAppointmentResponse> getUpcomingAppointments(String token) {
        UUID userId = extractUserIdFromToken(token);
        log.info("Fetching upcoming appointments for userId={}", userId);

        List<Appointment> appointments = appointmentRepository
                .findUpcomingAppointmentsByUserId(userId, LocalDate.now());

        if (appointments.isEmpty()) {
            log.info("Không có lịch khám sắp tới cho userId={}", userId);
            return Collections.emptyList();
        }

        Map<UUID, DoctorAvailableSlot> slotMap = doctorAvailableSlotRepository
                .findAllById(appointments.stream().map(Appointment::getSlotId).toList())
                .stream()
                .collect(Collectors.toMap(DoctorAvailableSlot::getSlotId, s -> s));

        Map<UUID, Doctor> doctorMap = doctorRepository
                .findAllById(appointments.stream().map(Appointment::getDoctorId).toList())
                .stream()
                .collect(Collectors.toMap(Doctor::getDoctorId, d -> d));

        return appointments.stream()
                .map(appointment -> {
                    DoctorAvailableSlot slot = slotMap.get(appointment.getSlotId());
                    Doctor doctor = doctorMap.get(appointment.getDoctorId());

                    String doctorName = "N/A";
                    String specialty = "N/A";
                    if (doctor != null) {
                        User doctorUser = userRepository.findById(doctor.getUserId()).orElse(null);
                        doctorName = doctorUser != null ? doctorUser.getFullName() : "N/A";
                    }

                    return PatientAppointmentResponse.builder()
                            .appointmentId(appointment.getAppointmentId())
                            .doctorName(doctorName)
                            .specialty(specialty)
                            .appointmentDate(appointment.getAppointmentDate())
                            .startTime(slot != null ? slot.getStartTime() : null)
                            .endTime(slot != null ? slot.getEndTime() : null)
                            .expectedTime(appointment.getExpectedTime())
                            .status(appointment.getStatus())
                            .notes(appointment.getNotes())
                            .createdAt(appointment.getCreatedAt())
                            .build();
                })
                .toList();
    }
    private UUID extractUserIdFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token.replace("Bearer ", ""));
            String userId = signedJWT.getJWTClaimsSet().getStringClaim("userId");
            return UUID.fromString(userId);
        } catch (Exception e) {
            log.error("Error extracting userId from token: {}", e.getMessage());
            throw new RuntimeException("Token không hợp lệ");
        }
    }

}
