package com.IMIC.booking_care.appointment.repository;

import com.IMIC.booking_care.appointment.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    @Query("""
        SELECT d.doctorId, d.userId, u.fullName, s.name, d.bio, d.experience, d.treatmentScope
                    FROM Doctor d
                    LEFT JOIN Specialty s ON d.specialtyId = s.specialtyId
                    LEFT JOIN User u ON d.userId = u.userId
    """)
    List<Object[]> findAllDoctor();
    @Query("""
        SELECT d.doctorId, d.userId, u.fullName, s.name, d.bio, d.experience, d.treatmentScope
        FROM Doctor d
        LEFT JOIN Specialty s ON d.specialtyId = s.specialtyId
        LEFT JOIN User u ON d.userId = u.userId
        WHERE d.specialtyId = :specialtyId
    """)
    List<Object[]> findAllBySpecialtyId(@Param("specialtyId") UUID specialtyId);
    Optional<Doctor> findByUserId(UUID userId);
}
