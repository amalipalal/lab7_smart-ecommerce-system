package com.example.ecommerce_system.service;

import com.example.ecommerce_system.dto.cache.CacheComparison;
import com.example.ecommerce_system.dto.cache.CacheSnapshot;
import com.example.ecommerce_system.dto.cache.PerformanceReport;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.AllArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CacheAnalysisService {

    private final CacheManager cacheManager;
    private final Map<String, CacheSnapshot> baselineSnapshots = new HashMap<>();

    public String captureBaseline() {
        baselineSnapshots.clear();
        for (String cacheName : cacheManager.getCacheNames()) {
            CacheSnapshot snapshot = getCurrentSnapshot(cacheName);
            baselineSnapshots.put(cacheName, snapshot);
        }
        return "Baseline captured for " + baselineSnapshots.size() + " caches at " + LocalDateTime.now();
    }

    public List<CacheSnapshot> getCurrentStats() {
        return cacheManager.getCacheNames()
                .stream()
                .map(this::getCurrentSnapshot)
                .collect(Collectors.toList());
    }

    public PerformanceReport getPerformanceComparison() {
        List<CacheComparison> comparisons = new ArrayList<>();
        long totalHits = 0;
        long totalMisses = 0;

        for (String cacheName : cacheManager.getCacheNames()) {
            CacheSnapshot baseline = baselineSnapshots.getOrDefault(cacheName, createEmptySnapshot(cacheName));
            CacheSnapshot current = getCurrentSnapshot(cacheName);

            CacheComparison comparison = buildComparison(cacheName, baseline, current);
            comparisons.add(comparison);

            totalHits += current.getHitCount();
            totalMisses += current.getMissCount();
        }

        double overallHitRate = calculateOverallHitRate(totalHits, totalMisses);

        return PerformanceReport.builder()
                .reportTime(LocalDateTime.now())
                .cacheComparisons(comparisons)
                .overallHitRate(overallHitRate)
                .totalHits(totalHits)
                .totalMisses(totalMisses)
                .summary(generateSummary(totalHits, totalMisses, overallHitRate))
                .topPerformers(getTopPerformingCachesList())
                .recommendations(getCacheRecommendationsList())
                .build();
    }

    public List<String> getTopPerformingCaches() {
        return cacheManager.getCacheNames().stream()
                .sorted((c1, c2) -> {
                    double hitRate1 = getCurrentSnapshot(c1).getHitRate();
                    double hitRate2 = getCurrentSnapshot(c2).getHitRate();
                    return Double.compare(hitRate2, hitRate1);
                })
                .collect(Collectors.toList());
    }

    public List<String> getCacheRecommendations() {
        return getCacheRecommendationsList();
    }

    public String resetCacheStats() {
        for (String cacheName : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                caffeineCache.clear();
            }
        }
        baselineSnapshots.clear();
        return "All cache statistics reset at " + LocalDateTime.now();
    }

    public String getPerformanceSummary() {
        long totalHits = 0;
        long totalMisses = 0;
        long totalEvictions = 0;
        double totalResponseTimeSaved = 0;

        for (String cacheName : cacheManager.getCacheNames()) {
            CacheSnapshot snapshot = getCurrentSnapshot(cacheName);
            totalHits += snapshot.getHitCount();
            totalMisses += snapshot.getMissCount();
            totalEvictions += snapshot.getEvictionCount();
            totalResponseTimeSaved += calculateTimeSaved(snapshot);
        }

        double hitRate = calculateOverallHitRate(totalHits, totalMisses);

        return String.format(
                "Cache Performance Analysis Summary:\n\n" +
                "Overall Statistics:\n" +
                "   - Hit Rate: %.1f%% (%d hits out of %d requests)\n" +
                "   - Database Queries Saved: %d\n" +
                "   - Estimated Response Time Saved: %.1f ms\n" +
                "   - Cache Evictions: %d\n\n" +
                "Impact:\n" +
                "   - Cache prevented %d database roundtrips\n" +
                "   - Performance improvement: %s\n" +
                "   - System efficiency: %s",
                hitRate * 100, totalHits, totalHits + totalMisses, totalHits,
                totalResponseTimeSaved, totalEvictions, totalHits,
                getPerformanceLevel(hitRate),
                getEfficiencyLevel(totalHits)
        );
    }

    public Optional<CacheComparison> getCacheAnalysis(String cacheName) {
        if (!cacheManager.getCacheNames().contains(cacheName)) {
            return Optional.empty();
        }

        CacheSnapshot baseline = baselineSnapshots.getOrDefault(cacheName, createEmptySnapshot(cacheName));
        CacheSnapshot current = getCurrentSnapshot(cacheName);

        return Optional.of(buildComparison(cacheName, baseline, current));
    }

    private CacheSnapshot getCurrentSnapshot(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache instanceof CaffeineCache caffeineCache) {
            CacheStats stats = caffeineCache.getNativeCache().stats();

            return CacheSnapshot.builder()
                    .cacheName(cacheName)
                    .hitCount(stats.hitCount())
                    .missCount(stats.missCount())
                    .hitRate(stats.hitRate())
                    .requestCount(stats.requestCount())
                    .loadCount(stats.loadCount())
                    .averageLoadTime(stats.averageLoadPenalty() / 1_000_000.0)
                    .evictionCount(stats.evictionCount())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        return CacheSnapshot.builder()
                .cacheName(cacheName)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private CacheSnapshot createEmptySnapshot(String cacheName) {
        return CacheSnapshot.builder()
                .cacheName(cacheName)
                .hitCount(0)
                .missCount(0)
                .hitRate(0.0)
                .requestCount(0)
                .build();
    }

    private CacheComparison buildComparison(String cacheName, CacheSnapshot baseline, CacheSnapshot current) {
        long hitDelta = current.getHitCount() - baseline.getHitCount();
        long missDelta = current.getMissCount() - baseline.getMissCount();
        double hitRateImprovement = current.getHitRate() - baseline.getHitRate();

        return CacheComparison.builder()
                .cacheName(cacheName)
                .baseline(baseline)
                .current(current)
                .hitCountDelta(hitDelta)
                .missCountDelta(missDelta)
                .hitRateImprovement(hitRateImprovement)
                .requestsSaved(hitDelta)
                .performanceGain(hitDelta > 0 ? (double) hitDelta / (hitDelta + missDelta) * 100 : 0)
                .build();
    }

    private List<String> getTopPerformingCachesList() {
        return getTopPerformingCaches();
    }

    private List<String> getCacheRecommendationsList() {
        List<String> recommendations = new ArrayList<>();

        for (String cacheName : cacheManager.getCacheNames()) {
            CacheSnapshot snapshot = getCurrentSnapshot(cacheName);

            if (snapshot.getHitRate() < 0.3 && snapshot.getRequestCount() > 10) {
                recommendations.add("WARNING: Cache '" + cacheName + "' has low hit rate (" +
                        String.format("%.1f%%", snapshot.getHitRate() * 100) +
                        "). Consider reviewing cache keys, TTL settings, or caching strategy.");
            }

            if (snapshot.getEvictionCount() > snapshot.getHitCount() * 0.1) {
                recommendations.add("INFO: Cache '" + cacheName + "' has high eviction rate (" +
                        snapshot.getEvictionCount() + " evictions). Consider increasing cache size or reducing TTL.");
            }

            if (snapshot.getHitCount() == 0 && snapshot.getRequestCount() > 5) {
                recommendations.add("ERROR: Cache '" + cacheName + "' is not providing any hits despite " +
                        snapshot.getRequestCount() + " requests. Review caching implementation.");
            }

            if (snapshot.getHitRate() > 0.9 && snapshot.getRequestCount() > 50) {
                recommendations.add("SUCCESS: Cache '" + cacheName + "' is performing excellently (" +
                        String.format("%.1f%%", snapshot.getHitRate() * 100) + " hit rate)!");
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("All caches are performing within acceptable ranges!");
        }

        return recommendations;
    }

    private double calculateOverallHitRate(long totalHits, long totalMisses) {
        return totalHits + totalMisses > 0 ? (double) totalHits / (totalHits + totalMisses) : 0.0;
    }

    private String generateSummary(long totalHits, long totalMisses, double hitRate) {
        long totalRequests = totalHits + totalMisses;
        return String.format(
                "Cache Performance: %.1f%% hit rate (%d/%d requests). " +
                "Cache prevented %d database queries. %s",
                hitRate * 100, totalHits, totalRequests, totalHits,
                hitRate > 0.7 ? "Excellent performance!" :
                hitRate > 0.5 ? "Good performance." :
                "Consider optimizing cache strategy."
        );
    }

    private double calculateTimeSaved(CacheSnapshot snapshot) {
        double cacheHitTime = 1.0;
        double databaseQueryTime = 50.0;
        return snapshot.getHitCount() * (databaseQueryTime - cacheHitTime);
    }

    private String getPerformanceLevel(double hitRate) {
        if (hitRate > 0.7) return "Excellent (>70%)";
        if (hitRate > 0.5) return "Good (50-70%)";
        if (hitRate > 0.3) return "Moderate (30-50%)";
        return "Needs Optimization (<30%)";
    }

    private String getEfficiencyLevel(long totalHits) {
        if (totalHits > 1000) return "High efficiency - cache is significantly reducing database load";
        if (totalHits > 100) return "Moderate efficiency - cache is providing good benefits";
        return "Low efficiency - consider reviewing caching strategy";
    }
}
