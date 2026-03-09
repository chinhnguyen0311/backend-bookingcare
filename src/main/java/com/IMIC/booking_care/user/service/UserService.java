package com.IMIC.booking_care.user.service;

import com.IMIC.booking_care.appointment.dto.response.AppointmentResponse;
import com.IMIC.booking_care.appointment.entity.Appointment;
import com.IMIC.booking_care.appointment.entity.Doctor;
import com.IMIC.booking_care.appointment.entity.DoctorAvailableSlot;
import com.IMIC.booking_care.appointment.enums.AppointmentStatus;
import com.IMIC.booking_care.appointment.repository.AppointmentRepository;
import com.IMIC.booking_care.appointment.repository.DoctorAvailableSlotRepository;
import com.IMIC.booking_care.appointment.repository.DoctorRepository;
import com.IMIC.booking_care.user.dto.request.UpdateProfileRequest;
import com.IMIC.booking_care.user.dto.response.PatientAppointmentResponse;
import com.IMIC.booking_care.user.dto.response.UserProfileResponse;
import com.IMIC.booking_care.user.entity.User;
import com.IMIC.booking_care.user.repository.UserRepository;
import com.nimbusds.jwt.SignedJWT;
import jakarta.transaction.Transactional;
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

    @Transactional
    public AppointmentResponse cancelAppointment(String token, UUID appointmentId) {
        UUID userId = extractUserIdFromToken(token);
        log.info("Cancelling appointmentId={} for userId={}", appointmentId, userId);

        // 1. Tìm appointment — kiểm tra đúng của user này
        Appointment appointment = appointmentRepository.findByAppointmentIdAndUserId(appointmentId, userId)
                .orElseThrow(() -> new RuntimeException("Lịch hẹn không tồn tại"));

        // 2. Kiểm tra status
        if (AppointmentStatus.CANCELLED.equals(appointment.getStatus())) {
            throw new RuntimeException("Lịch hẹn này đã bị huỷ trước đó");
        }

        if (AppointmentStatus.COMPLETED.equals(appointment.getStatus())) {
            throw new RuntimeException("Không thể huỷ lịch hẹn đã hoàn thành");
        }

        // 3. Cập nhật status → CANCELLED
        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);

        // 4. Mở lại slot
        DoctorAvailableSlot slot = doctorAvailableSlotRepository.findById(appointment.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot không tồn tại"));

        slot.setIsAvailable(true);
        doctorAvailableSlotRepository.save(slot);

        log.info("Appointment {} cancelled by userId={}, slot {} is now available",
                appointmentId, userId, slot.getSlotId());

        return AppointmentResponse.builder()
                .appointmentId(saved.getAppointmentId())
                .doctorId(saved.getDoctorId())
                .appointmentDate(saved.getAppointmentDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .expectedTime(saved.getExpectedTime())
                .status(saved.getStatus())
                .notes(saved.getNotes())
                .createdAt(saved.getCreatedAt())
                .build();
    }
    public UserProfileResponse getProfile(String token) {
        UUID userId = extractUserIdFromToken(token);
        log.info("Fetching profile for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .dob(user.getDob())
                .gender(user.getGender())
                .address(user.getAddress())
                .build();
    }
    @Transactional
    public UserProfileResponse updateProfile(String token, UpdateProfileRequest request) {
        UUID userId = extractUserIdFromToken(token);
        log.info("Updating profile for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getDob() != null) user.setDob(request.getDob());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        User saved = userRepository.save(user);
        log.info("Profile updated for userId={}", userId);

        return UserProfileResponse.builder()
                .userId(saved.getUserId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .phoneNumber(saved.getPhoneNumber())
                .dob(saved.getDob())
                .gender(saved.getGender())
                .address(saved.getAddress())
                .build();
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
