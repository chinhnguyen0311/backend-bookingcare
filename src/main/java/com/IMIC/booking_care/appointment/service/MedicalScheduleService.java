package com.IMIC.booking_care.appointment.service;

import com.IMIC.booking_care.appointment.dto.request.MedicalScheduleSlotRequest;
import com.IMIC.booking_care.appointment.dto.response.MedicalScheduleSlotResponse;
import com.IMIC.booking_care.appointment.entity.DoctorAvailableSlot;
import com.IMIC.booking_care.appointment.repository.DoctorAvailableSlotRepository;
import com.IMIC.booking_care.appointment.repository.DoctorSchedulesRepository;
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
}
