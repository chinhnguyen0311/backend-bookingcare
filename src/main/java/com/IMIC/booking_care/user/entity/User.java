package com.IMIC.booking_care.user.entity;

import com.IMIC.booking_care.authentication.enums.UserRole;
import com.IMIC.booking_care.user.enums.Gender;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "username", length = 100, unique = true, nullable = false)
    private String username;

    @Column(name = "email", length = 255, unique = true, nullable = false)
    private String email;

    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'PATIENT'")
    private UserRole role;

    @Column(name = "google_id", length = 255)
    private String googleId;

    @Column(name = "email_verified", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean emailVerified;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
        if (role == null) {
            role = UserRole.PATIENT;
        }
        if (emailVerified == null) {
            emailVerified = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}