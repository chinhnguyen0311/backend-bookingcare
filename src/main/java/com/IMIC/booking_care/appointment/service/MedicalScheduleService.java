package com.IMIC.booking_care.appointment.service;

import com.IMIC.booking_care.appointment.dto.request.AppointmentRequest;
import com.IMIC.booking_care.appointment.dto.request.MedicalScheduleSlotRequest;
import com.IMIC.booking_care.appointment.dto.response.AppointmentResponse;
import com.IMIC.booking_care.appointment.dto.response.MedicalScheduleSlotResponse;
import com.IMIC.booking_care.appointment.entity.Appointment;
import com.IMIC.booking_care.appointment.entity.DoctorAvailableSlot;
import com.IMIC.booking_care.appointment.repository.AppointmentRepository;
import com.IMIC.booking_care.appointment.repository.DoctorAvailableSlotRepository;
import com.IMIC.booking_care.appointment.repository.DoctorSchedulesRepository;
import com.IMIC.booking_care.user.entity.User;
import com.IMIC.booking_care.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MedicalScheduleService {
    DoctorSchedulesRepository doctorSchedulesRepository;
    DoctorAvailableSlotRepository doctorAvailableSlotRepository;
    UserRepository userRepository;
    AppointmentRepository appointmentRepository;
    public List<MedicalScheduleSlotResponse> getMedicalScheduleSlot(MedicalScheduleSlotRequest request){
        log.info("Fetching slots for doctorId={} on date={}", request.getDoctorId(), request.getSlotDate());

        List<DoctorAvailableSlot> slots = doctorAvailableSlotRepository
                .findByDoctorIdAndSlotDate(request.getDoctorId(), request.getSlotDate());

        if (slots.isEmpty()) {
            log.warn("No slots found for doctorId={} on date={}", request.getDoctorId(), request.getSlotDate());
            throw new RuntimeException("No available slots found");
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        return slots.stream()
                .filter(slot -> {
                    // Nếu là ngày hôm nay → chỉ lấy slot có startTime sau giờ hiện tại
                    if (request.getSlotDate().equals(today)) {
                        return slot.getStartTime().isAfter(now);
                    }
                    // Ngày khác → lấy tất cả
                    return true;
                })
                .map(slot -> MedicalScheduleSlotResponse.builder()
                        .doctorId(slot.getDoctorId())
                        .slotId(slot.getSlotId())
                        .slotDate(slot.getSlotDate())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .isAvailable(slot.getIsAvailable())
                        .build())
                .toList();
    }
    public List<LocalDate> getWorkingDates(UUID doctorId) {
        log.info("Fetching working dates for doctorId={}", doctorId);

        List<LocalDate> dates = doctorAvailableSlotRepository
                .findWorkingDatesByDoctorId(doctorId, LocalDate.now());

        if (dates.isEmpty()) {
            log.info("Bác sĩ {} không có lịch làm việc", doctorId);
        }

        return dates;
    }
    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        log.info("Booking appointment for doctor={} on date={}", request.getDoctorId(), request.getAppointmentDate());

        // 1. Kiểm tra slot có tồn tại và còn trống không
        DoctorAvailableSlot slot = doctorAvailableSlotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot không tồn tại"));

        if (!slot.getIsAvailable()) {
            throw new RuntimeException("Slot này đã được đặt");
        }

        if (!slot.getDoctorId().equals(request.getDoctorId())) {
            throw new RuntimeException("Slot không thuộc bác sĩ này");
        }

        // 2. Tạo user tạm từ thông tin form
        User user = userRepository.save(User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .username(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .dob(request.getDob())
                .address(request.getAddress())
                .gender(request.getGender())
                .build());

        log.info("Created temporary user with userId={}", user.getUserId());

        // 3. Tạo appointment
        Appointment appointment = Appointment.builder()
                .userId(user.getUserId())
                .doctorId(request.getDoctorId())
                .slotId(request.getSlotId())
                .appointmentDate(request.getAppointmentDate())
                .expectedTime(slot.getStartTime())
                .notes(request.getNotes())
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment created with appointmentId={}", saved.getAppointmentId());

        // 4. Đánh dấu slot đã được đặt
        slot.setIsAvailable(false);
        doctorAvailableSlotRepository.save(slot);

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
