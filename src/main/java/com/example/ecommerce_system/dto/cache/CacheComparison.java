package com.example.ecommerce_system.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheComparison {
    private String cacheName;
    private CacheSnapshot baseline;
    private CacheSnapshot current;
    private long hitCountDelta;
    private long missCountDelta;
    private double hitRateImprovement;
    private long requestsSaved;
    private double performanceGain;
}
