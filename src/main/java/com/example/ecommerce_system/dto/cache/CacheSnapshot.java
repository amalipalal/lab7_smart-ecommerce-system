package com.example.ecommerce_system.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheSnapshot {
    private String cacheName;
    private long hitCount;
    private long missCount;
    private double hitRate;
    private long requestCount;
    private long loadCount;
    private double averageLoadTime;
    private long evictionCount;
    private LocalDateTime timestamp;
}
