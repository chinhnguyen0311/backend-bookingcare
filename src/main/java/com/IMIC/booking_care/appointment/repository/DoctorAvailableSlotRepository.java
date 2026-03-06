package com.IMIC.booking_care.appointment.repository;

import com.IMIC.booking_care.appointment.entity.DoctorAvailableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorAvailableSlotRepository extends JpaRepository<DoctorAvailableSlot, UUID> {
    List<DoctorAvailableSlot> findByDoctorIdAndSlotDate(UUID doctorId, LocalDate slotDate);
}
