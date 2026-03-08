package com.IMIC.booking_care.appointment.repository;

import com.IMIC.booking_care.appointment.entity.Appointment;
import com.IMIC.booking_care.appointment.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    @Query("""
    SELECT a FROM Appointment a
    WHERE a.doctorId = :doctorId
    AND a.appointmentDate = :today
    AND a.status IN ('CONFIRMED', 'COMPLETED')
    ORDER BY a.expectedTime ASC
""")
    List<Appointment> findTodayAppointmentsByDoctorId(
            @Param("doctorId") UUID doctorId,
            @Param("today") LocalDate today
    );
    @Query("""
    SELECT a FROM Appointment a
    WHERE a.doctorId = :doctorId
    AND a.status = :status
    ORDER BY a.appointmentDate ASC, a.expectedTime ASC
""")
    List<Appointment> findAppointmentsByDoctorIdAndStatus(
            @Param("doctorId") UUID doctorId,
            @Param("status") AppointmentStatus status
    );
    @Query("""
    SELECT a FROM Appointment a
    WHERE a.appointmentId = :appointmentId
    AND a.doctorId = :doctorId
""")
    Optional<Appointment> findByAppointmentIdAndDoctorId(
            @Param("appointmentId") UUID appointmentId,
            @Param("doctorId") UUID doctorId
    );
    @Query("""
    SELECT a FROM Appointment a
    JOIN DoctorAvailableSlot s ON a.slotId = s.slotId
    WHERE a.doctorId = :doctorId
    AND a.appointmentDate = :today
    AND a.status = 'CONFIRMED'
    ORDER BY s.startTime ASC
""")
    List<Appointment> findConfirmedTodayOrderByTime(
            @Param("doctorId") UUID doctorId,
            @Param("today") LocalDate today
    );
    @Query("""
    SELECT a FROM Appointment a
    WHERE a.userId = :userId
    AND a.status = :status
    ORDER BY a.appointmentDate ASC, a.expectedTime ASC
""")
    List<Appointment> findAppointmentsByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") AppointmentStatus status
    );
    @Query("""
    SELECT a FROM Appointment a
    WHERE a.userId = :userId
    AND a.appointmentDate >= :today
    AND a.status IN ('CONFIRMED', 'PENDING')
    ORDER BY a.appointmentDate ASC, a.expectedTime ASC
""")
    List<Appointment> findUpcomingAppointmentsByUserId(
            @Param("userId") UUID userId,
            @Param("today") LocalDate today
    );
}
