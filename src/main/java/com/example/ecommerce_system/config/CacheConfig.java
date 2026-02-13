package com.example.ecommerce_system.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Profile("dev")
    public CacheManager devCacheManager() {
        return createCacheManager(Duration.ofMinutes(5), 500);
    }

    @Bean
    @Profile("prod")
    public CacheManager prodCacheManager() {
        return createCacheManager(Duration.ofMinutes(15), 2000);
    }

    @Bean
    @Profile("test")
    public CacheManager testCacheManager() {
        return createCacheManager(Duration.ofMinutes(1), 100);
    }

    private CacheManager createCacheManager(Duration baseTtl, int baseSize) {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
            buildCache("categories", baseTtl, 2, baseSize, 2),
            buildCache("products", baseTtl, 2, baseSize, 4),
            buildCache("users", baseTtl, 3, baseSize, 1),
            buildCache("customers", baseTtl, 3, baseSize, 2),
            buildCache("orders", baseTtl, 2, baseSize, 4),
            buildCache("order_items", baseTtl, 2, baseSize, 10),
            buildCache("carts", baseTtl, 1, baseSize, 2),
            buildCache("reviews", baseTtl, 2, baseSize, 6),
            buildCache("paginated", baseTtl.dividedBy(2), baseSize, 3)
        ));

        return cacheManager;
    }

    private CaffeineCache buildCache(String name, Duration baseTtl, int ttlMultiplier, int baseSize, int sizeMultiplier) {
        return new CaffeineCache(name, Caffeine.newBuilder()
            .expireAfterWrite(baseTtl.multipliedBy(ttlMultiplier))
            .maximumSize((long) baseSize * sizeMultiplier)
            .recordStats()
            .build());
    }

    private CaffeineCache buildCache(String name, Duration ttl, int baseSize, int sizeMultiplier) {
        return new CaffeineCache(name, Caffeine.newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize((long) baseSize * sizeMultiplier)
            .recordStats()
            .build());
    }
}
