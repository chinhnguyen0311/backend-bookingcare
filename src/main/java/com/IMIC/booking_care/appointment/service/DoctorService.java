package com.IMIC.booking_care.appointment.service;

import com.IMIC.booking_care.appointment.dto.response.AppointmentResponse;
import com.IMIC.booking_care.appointment.dto.response.DoctorResponse;
import com.IMIC.booking_care.appointment.dto.response.TodayAppointmentResponse;
import com.IMIC.booking_care.appointment.entity.Appointment;
import com.IMIC.booking_care.appointment.entity.Doctor;
import com.IMIC.booking_care.appointment.entity.DoctorAvailableSlot;
import com.IMIC.booking_care.appointment.enums.AppointmentStatus;
import com.IMIC.booking_care.appointment.repository.AppointmentRepository;
import com.IMIC.booking_care.appointment.repository.DoctorAvailableSlotRepository;
import com.IMIC.booking_care.appointment.repository.DoctorRepository;
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
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DoctorService {
    DoctorRepository doctorRepository;
    UserRepository userRepository;
    AppointmentRepository appointmentRepository;
    DoctorAvailableSlotRepository doctorAvailableSlotRepository;
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
    public List<TodayAppointmentResponse> getTodayAppointments(String token) {
        // 1. Lấy userId từ token
        UUID userId = extractUserIdFromToken(token);
        log.info("Fetching today appointments for userId={}", userId);

        // 2. Tìm doctorId từ userId
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không phải bác sĩ"));

        UUID doctorId = doctor.getDoctorId();
        log.info("Resolved doctorId={} from userId={}", doctorId, userId);

        // 3. Lấy danh sách appointment hôm nay
        List<Appointment> appointments = appointmentRepository
                .findTodayAppointmentsByDoctorId(doctorId, LocalDate.of(2026, 3, 9));

        if (appointments.isEmpty()) {
            log.info("Không có lịch khám nào hôm nay cho doctorId={}", doctorId);
            return Collections.emptyList();
        }

        // 4. Map sang response kèm thông tin bệnh nhân
        return appointments.stream()
                .map(appointment -> {
                    User patient = userRepository.findById(appointment.getUserId())
                            .orElse(null);

                    return TodayAppointmentResponse.builder()
                            .appointmentId(appointment.getAppointmentId())
                            .patientName(patient != null ? patient.getFullName() : "N/A")
                            .phoneNumber(patient != null ? patient.getPhoneNumber() : "N/A")
                            .email(patient != null ? patient.getEmail() : "N/A")
                            .dob(patient != null ? patient.getDob() : null)
                            .address(patient != null ? patient.getAddress() : "N/A")
                            .gender(patient != null ? patient.getGender() : null)
                            .appointmentDate(appointment.getAppointmentDate())
                            .expectedTime(appointment.getExpectedTime())
                            .status(appointment.getStatus())
                            .notes(appointment.getNotes())
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
    public List<TodayAppointmentResponse> getAppointmentsByStatus(String token, AppointmentStatus status) {
        UUID userId = extractUserIdFromToken(token);

        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không phải bác sĩ"));

        UUID doctorId = doctor.getDoctorId();
        log.info("Fetching {} appointments for doctorId={}", status, doctorId);

        List<Appointment> appointments = appointmentRepository
                .findAppointmentsByDoctorIdAndStatus(doctorId, status);

        if (appointments.isEmpty()) {
            log.info("Không có lịch khám {} nào cho doctorId={}", status, doctorId);
            return Collections.emptyList();
        }

        return appointments.stream()
                .map(appointment -> {
                    User patient = userRepository.findById(appointment.getUserId())
                            .orElse(null);

                    return TodayAppointmentResponse.builder()
                            .appointmentId(appointment.getAppointmentId())
                            .patientName(patient != null ? patient.getFullName() : "N/A")
                            .phoneNumber(patient != null ? patient.getPhoneNumber() : "N/A")
                            .email(patient != null ? patient.getEmail() : "N/A")
                            .dob(patient != null ? patient.getDob() : null)
                            .address(patient != null ? patient.getAddress() : "N/A")
                            .gender(patient != null ? patient.getGender() : null)
                            .appointmentDate(appointment.getAppointmentDate())
                            .expectedTime(appointment.getExpectedTime())
                            .status(appointment.getStatus())
                            .notes(appointment.getNotes())
                            .build();
                })
                .toList();
    }
    @Transactional
    public AppointmentResponse confirmAppointment(String token, UUID appointmentId) {
        UUID userId = extractUserIdFromToken(token);

        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không phải bác sĩ"));

        UUID doctorId = doctor.getDoctorId();
        log.info("Confirming appointmentId={} for doctorId={}", appointmentId, doctorId);

        Appointment appointment = appointmentRepository
                .findByAppointmentIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new RuntimeException("Lịch hẹn không tồn tại"));

        if (!AppointmentStatus.PENDING.equals(appointment.getStatus())) {
            throw new RuntimeException("Chỉ có thể xác nhận lịch hẹn đang ở trạng thái PENDING");
        }


        DoctorAvailableSlot slot = doctorAvailableSlotRepository.findById(appointment.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot không tồn tại"));

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment saved = appointmentRepository.save(appointment);

        log.info("Appointment {} confirmed successfully", appointmentId);

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
    @Transactional
    public AppointmentResponse cancelAppointment(String token, UUID appointmentId) {
        // 1. Lấy doctorId từ token
        UUID userId = extractUserIdFromToken(token);

        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không phải bác sĩ"));

        UUID doctorId = doctor.getDoctorId();
        log.info("Cancelling appointmentId={} for doctorId={}", appointmentId, doctorId);

        // 2. Tìm appointment
        Appointment appointment = appointmentRepository
                .findByAppointmentIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new RuntimeException("Lịch hẹn không tồn tại"));

        // 3. Kiểm tra status — chỉ PENDING hoặc CONFIRMED mới được huỷ
        if (AppointmentStatus.CANCELLED.equals(appointment.getStatus())) {
            throw new RuntimeException("Lịch hẹn này đã bị huỷ trước đó");
        }

        if (AppointmentStatus.COMPLETED.equals(appointment.getStatus())) {
            throw new RuntimeException("Không thể huỷ lịch hẹn đã hoàn thành");
        }

        // 4. Cập nhật status → CANCELLED
        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);

        // 5. Mở lại slot để người khác có thể đặt
        DoctorAvailableSlot slot = doctorAvailableSlotRepository.findById(appointment.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot không tồn tại"));

        slot.setIsAvailable(true);
        doctorAvailableSlotRepository.save(slot);

        log.info("Appointment {} cancelled successfully, slot {} is now available", appointmentId, slot.getSlotId());

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
    public TodayAppointmentResponse getNextAppointment(String token) {
        UUID userId = extractUserIdFromToken(token);

        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không phải bác sĩ"));

        UUID doctorId = doctor.getDoctorId();
        LocalDate today = LocalDate.of(2026,3,9);
        LocalTime now = LocalTime.of(7,0,0);

        log.info("Fetching next appointment for doctorId={} at {}", doctorId, now);

        // Lấy tất cả ca CONFIRMED hôm nay sắp xếp theo giờ tăng dần
        List<Appointment> confirmedList = appointmentRepository
                .findConfirmedTodayOrderByTime(doctorId, today);

        if (confirmedList.isEmpty()) {
            log.info("Không còn ca CONFIRMED nào hôm nay cho doctorId={}", doctorId);
            return null;
        }

        // Lấy slot để so sánh thời gian
        Map<UUID, DoctorAvailableSlot> slotMap = doctorAvailableSlotRepository
                .findAllById(confirmedList.stream().map(Appointment::getSlotId).toList())
                .stream()
                .collect(Collectors.toMap(DoctorAvailableSlot::getSlotId, s -> s));

        // Tìm ca đầu tiên có endTime > now (chưa kết thúc)
        Appointment next = confirmedList.stream()
                .filter(a -> {
                    DoctorAvailableSlot slot = slotMap.get(a.getSlotId());
                    return slot != null && slot.getEndTime().isAfter(now);
                })
                .findFirst()
                .orElse(null);

        if (next == null) {
            log.info("Tất cả ca hôm nay đã kết thúc cho doctorId={}", doctorId);
            return null;
        }

        DoctorAvailableSlot slot = slotMap.get(next.getSlotId());
        User patient = userRepository.findById(next.getUserId()).orElse(null);

        return TodayAppointmentResponse.builder()
                .appointmentId(next.getAppointmentId())
                .patientName(patient != null ? patient.getFullName() : "N/A")
                .phoneNumber(patient != null ? patient.getPhoneNumber() : "N/A")
                .email(patient != null ? patient.getEmail() : "N/A")
                .dob(patient != null ? patient.getDob() : null)
                .address(patient != null ? patient.getAddress() : "N/A")
                .gender(patient != null ? patient.getGender() : null)
                .appointmentDate(next.getAppointmentDate())
                .expectedTime(next.getExpectedTime())
                .status(next.getStatus())
                .notes(next.getNotes())
                .build();
    }
    @Transactional
    public AppointmentResponse completeAppointment(String token, UUID appointmentId) {
        UUID userId = extractUserIdFromToken(token);

        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không phải bác sĩ"));

        UUID doctorId = doctor.getDoctorId();
        log.info("Completing appointmentId={} for doctorId={}", appointmentId, doctorId);

        Appointment appointment = appointmentRepository
                .findByAppointmentIdAndDoctorId(appointmentId, doctorId)
                .orElseThrow(() -> new RuntimeException("Lịch hẹn không tồn tại"));

        // Chỉ CONFIRMED mới được complete
        if (!AppointmentStatus.CONFIRMED.equals(appointment.getStatus())) {
            throw new RuntimeException("Chỉ có thể hoàn thành lịch hẹn đang ở trạng thái CONFIRMED");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment saved = appointmentRepository.save(appointment);

        DoctorAvailableSlot slot = doctorAvailableSlotRepository.findById(appointment.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot không tồn tại"));

        log.info("Appointment {} completed successfully", appointmentId);

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
}
