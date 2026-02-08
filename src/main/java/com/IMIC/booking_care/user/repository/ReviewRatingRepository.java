package com.IMIC.booking_care.user.repository;

import com.IMIC.booking_care.user.entity.ReviewRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRatingRepository extends JpaRepository<ReviewRating, UUID> {
}
