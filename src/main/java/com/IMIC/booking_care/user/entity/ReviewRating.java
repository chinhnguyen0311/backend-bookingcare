package com.IMIC.booking_care.user.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "review_ratings")
public class ReviewRating {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "rating_id", columnDefinition = "uuid")
    private UUID ratingId;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    @NotNull
    private UUID userId;

    @Column(name = "doctor_id", columnDefinition = "uuid", nullable = false)
    @NotNull
    private UUID doctorId;

    @Column(name = "rating")
    @Min(1)
    @Max(5)
    private Integer rating;

    @Column(name = "review", columnDefinition = "TEXT")
    @Size(max = 5000, message = "Review must not exceed 5000 characters")
    private String review;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}