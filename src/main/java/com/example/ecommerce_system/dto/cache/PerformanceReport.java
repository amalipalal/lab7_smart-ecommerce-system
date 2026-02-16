package com.example.ecommerce_system.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReport {
    private LocalDateTime reportTime;
    private List<CacheComparison> cacheComparisons;
    private double overallHitRate;
    private long totalHits;
    private long totalMisses;
    private String summary;
    private List<String> topPerformers;
    private List<String> recommendations;
}
