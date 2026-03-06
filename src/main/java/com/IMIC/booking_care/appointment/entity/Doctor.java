package com.IMIC.booking_care.appointment.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "doctor_id", columnDefinition = "uuid")
    UUID doctorId;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false, unique = true)
    UUID userId;

    @Column(name = "specialty_id", columnDefinition = "uuid")
    UUID specialtyId;

    @Column(name = "bio", columnDefinition = "TEXT")
    String bio;

    @Column(name = "experience", columnDefinition = "TEXT")
    String experience;

    @Column(name = "treatment_scope", columnDefinition = "TEXT")
    String treatmentScope;
}