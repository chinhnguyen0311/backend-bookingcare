package com.IMIC.booking_care.authentication.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory blacklist for revoked access token JTI.
 * Used on logout to invalidate the current access token until it expires.
 */
@Service
public class TokenBlacklistService {

    private final Set<String> blacklistedJti = ConcurrentHashMap.newKeySet();

    public void blacklist(String jti) {
        blacklistedJti.add(jti);
    }

    public boolean isBlacklisted(String jti) {
        return blacklistedJti.contains(jti);
    }
}
