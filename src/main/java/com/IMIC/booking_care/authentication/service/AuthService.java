package com.IMIC.booking_care.authentication.service;

import com.IMIC.booking_care.authentication.dto.request.AuthRequest;
import com.IMIC.booking_care.authentication.dto.request.ChangePasswordRequest;
import com.IMIC.booking_care.authentication.dto.request.CreateUserRequest;
import com.IMIC.booking_care.authentication.dto.request.LogoutRequest;
import com.IMIC.booking_care.authentication.dto.request.RegisterRequest;
import com.IMIC.booking_care.authentication.dto.response.AuthResponse;
import com.IMIC.booking_care.authentication.entity.RefreshToken;
import com.IMIC.booking_care.authentication.enums.UserRole;
import com.IMIC.booking_care.authentication.repository.RefreshTokenRepository;
import com.IMIC.booking_care.common.exception.CustomException;
import com.IMIC.booking_care.common.exception.ErrorCode;
import com.IMIC.booking_care.user.entity.User;
import com.IMIC.booking_care.user.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class AuthService {

    final UserRepository userRepository;
    final RefreshTokenRepository refreshTokenRepository;
    final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.signerKey}")
    String signerKey;

    @Value("${jwt.valid-duration}")
    long validDuration;

    @Value("${jwt.refreshable-duration}")
    long refreshableDuration;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Register attempt for username={}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new CustomException(ErrorCode.USERNAME_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_EXISTS);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(request.getPassword())
                .phoneNumber(request.getPhoneNumber())
                .role(UserRole.PATIENT)
                .build();

        user = userRepository.save(user);
        log.info("Register successful for userId={}", user.getUserId());

        return login(AuthRequest.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build());
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        log.info("Login attempt for username={}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getPasswordHash() == null || !user.getPasswordHash().equals(request.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = generateAccessToken(user);
        String refreshTokenValue = generateRefreshToken(user);

        Instant expiresAt = Instant.now().plus(refreshableDuration, ChronoUnit.SECONDS);
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(user.getUserId())
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        log.info("Login successful for username={}", request.getUsername());

        return AuthResponse.builder()
                .userId(user.getUserId().toString())
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public void logout(LogoutRequest request, String bearerToken) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                    .ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
        }

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String accessToken = bearerToken.substring(7).trim();
            try {
                JWTClaimsSet claims = parseAndValidateAccessToken(accessToken);
                String jti = claims.getJWTID();
                if (jti != null) {
                    tokenBlacklistService.blacklist(jti);
                }
            } catch (Exception e) {
                log.debug("Could not blacklist access token on logout: {}", e.getMessage());
            }
        }
        log.info("Logout completed");
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        String accessToken = bearerToken.substring(7).trim();
        JWTClaimsSet claims = parseAndValidateAccessToken(accessToken);
        String userIdStr = claims.getSubject();
        if (userIdStr == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getPasswordHash() == null || !user.getPasswordHash().equals(request.getOldPassword())) {
            throw new CustomException(ErrorCode.INVALID_OLD_PASSWORD);
        }

        user.setPasswordHash(request.getNewPassword());
        userRepository.save(user);

        refreshTokenRepository.deleteAllByUserId(user.getUserId());
        log.info("Password changed and refresh tokens revoked for userId={}", user.getUserId());
    }

    /**
     * Tạo tài khoản user (DOCTOR hoặc ADMIN). Chỉ ADMIN đã đăng nhập mới gọi được.
     */
    @Transactional
    public AuthResponse createUserByAdmin(CreateUserRequest request, String bearerToken) {
        requireAdmin(bearerToken);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new CustomException(ErrorCode.USERNAME_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_EXISTS);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(request.getPassword())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .build();

        user = userRepository.save(user);
        log.info("Admin created user userId={} role={}", user.getUserId(), user.getRole());

        return AuthResponse.builder()
                .userId(user.getUserId().toString())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    /**
     * Lấy user hiện tại từ Bearer token. Nếu không phải ADMIN thì ném ACCESS_DENIED.
     */
    public User requireAdmin(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        String accessToken = bearerToken.substring(7).trim();
        JWTClaimsSet claims = parseAndValidateAccessToken(accessToken);
        String userIdStr = claims.getSubject();
        if (userIdStr == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        User currentUser = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!UserRole.ADMIN.equals(currentUser.getRole())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        return currentUser;
    }

    private String generateAccessToken(User user) {
        String jti = UUID.randomUUID().toString();
        return generateToken(user, validDuration, jti);
    }

    private String generateRefreshToken(User user) {
        return generateToken(user, refreshableDuration, null);
    }

    private String generateToken(User user, long duration, String jti) {
        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(user.getUserId().toString())
                    .issuer("bookingcare")
                    .issueTime(new Date())
                    .expirationTime(new Date(Instant.now().plus(duration, ChronoUnit.SECONDS).toEpochMilli()))
                    .claim("userId", user.getUserId().toString())
                    .claim("username", user.getUsername())
                    .claim("role", user.getRole().name())
                    .claim("fullName", user.getFullName());
            if (jti != null) {
                builder.jwtID(jti);
            }
            JWTClaimsSet claimsSet = builder.build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new MACSigner(signerKey.getBytes()));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("Error generating token: {}", e.getMessage());
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể tạo token");
        }
    }

    /**
     * Parse and validate access token (signature + expiry + blacklist). Returns claims or throws.
     */
    public JWTClaimsSet parseAndValidateAccessToken(String accessToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(accessToken);
            if (!signedJWT.verify(new MACVerifier(signerKey.getBytes()))) {
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime().before(new Date())) {
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }
            String jti = claims.getJWTID();
            if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }
            return claims;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Invalid access token: {}", e.getMessage());
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }
}
