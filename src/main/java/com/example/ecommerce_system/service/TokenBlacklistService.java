package com.example.ecommerce_system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TokenBlacklistService {

    private final CacheManager cacheManager;

    public void blacklistToken(String jti) {
        var cache = cacheManager.getCache("tokenBlacklist");
        if (cache != null) {
            cache.put(jti, true);
        }
    }

    public boolean isBlacklisted(String jti) {
        var cache = cacheManager.getCache("tokenBlacklist");
        if (cache != null) {
            return cache.get(jti, Boolean.class) != null;
        }
        return false;
    }
}
