package com.IMIC.booking_care.authentication.repository;

import com.IMIC.booking_care.authentication.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    void deleteAllByUserId(UUID userId);
}
