package com.IMIC.booking_care.authentication.service;

import com.IMIC.booking_care.authentication.dto.request.AuthRequest;
import com.IMIC.booking_care.authentication.dto.response.AuthResponse;
import com.IMIC.booking_care.authentication.enums.UserRole;
import com.IMIC.booking_care.user.entity.User;
import com.IMIC.booking_care.user.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class AuthService {
    final UserRepository userRepository;

    @Value("${jwt.signerKey}")
    String signerKey;

    @Value("${jwt.valid-duration}")
    long validDuration;

    @Value("${jwt.refreshable-duration}")
    long refreshableDuration;

    public AuthResponse login(AuthRequest request) {
        log.info("Login attempt for username={}", request.getUsername());

        // 1. Tìm user theo username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        // 2. Kiểm tra role DOCTOR
        if (!UserRole.DOCTOR.equals(user.getRole())) {
            throw new RuntimeException("Tài khoản không có quyền truy cập");
        }

        // 3. Kiểm tra mật khẩu (không mã hoá)
        if (!request.getPassword().equals(user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu không chính xác");
        }

        // 4. Generate token
        String accessToken = generateToken(user, validDuration);
        String refreshToken = generateToken(user, refreshableDuration);

        log.info("Login successful for username={}", request.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .build();
    }

    private String generateToken(User user, long duration) {
        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getUserId().toString())
                    .issuer("bookingcare")
                    .issueTime(new Date())
                    .expirationTime(new Date(
                            Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()
                    ))
                    .claim("userId", user.getUserId().toString())
                    .claim("username", user.getUsername())
                    .claim("role", user.getRole().name())
                    .claim("fullName", user.getFullName())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new MACSigner(signerKey.getBytes()));

            return signedJWT.serialize();

        } catch (JOSEException e) {
            log.error("Error generating token: {}", e.getMessage());
            throw new RuntimeException("Không thể tạo token");
        }
    }
}
