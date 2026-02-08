package com.IMIC.booking_care.appointment.repository;

import com.IMIC.booking_care.appointment.entity.DoctorSchedules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DoctorSchedulesRepository extends JpaRepository<DoctorSchedules, UUID> {
}
