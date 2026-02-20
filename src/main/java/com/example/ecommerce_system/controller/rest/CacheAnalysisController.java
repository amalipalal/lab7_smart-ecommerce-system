package com.example.ecommerce_system.controller.rest;

import com.example.ecommerce_system.dto.cache.CacheComparison;
import com.example.ecommerce_system.dto.cache.CacheSnapshot;
import com.example.ecommerce_system.dto.cache.PerformanceReport;
import com.example.ecommerce_system.service.CacheAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/cache-analysis")
@PreAuthorize("hasRole('ADMIN')")
@AllArgsConstructor
@Tag(name = "Cache Analysis", description = "Endpoints for analyzing cache performance and statistics")
public class CacheAnalysisController {

    private final CacheAnalysisService cacheAnalysisService;

    @Operation(summary = "Capture baseline performance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Baseline captured successfully")
    })
    @PostMapping("/baseline")
    public ResponseEntity<String> captureBaseline() {
        String result = cacheAnalysisService.captureBaseline();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get current cache statistics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current statistics retrieved")
    })
    @GetMapping("/current-stats")
    public ResponseEntity<List<CacheSnapshot>> getCurrentStats() {
        List<CacheSnapshot> stats = cacheAnalysisService.getCurrentStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get comprehensive performance comparison")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Performance comparison retrieved")
    })
    @GetMapping("/comparison")
    public ResponseEntity<PerformanceReport> getPerformanceComparison() {
        PerformanceReport report = cacheAnalysisService.getPerformanceComparison();
        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Get top performing caches by hit rate")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Top performers retrieved")
    })
    @GetMapping("/top-performers")
    public ResponseEntity<List<String>> getTopPerformingCaches() {
        List<String> topCaches = cacheAnalysisService.getTopPerformingCaches();
        return ResponseEntity.ok(topCaches);
    }

    @Operation(summary = "Get cache optimization recommendations")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommendations retrieved")
    })
    @GetMapping("/recommendations")
    public ResponseEntity<List<String>> getCacheRecommendations() {
        List<String> recommendations = cacheAnalysisService.getCacheRecommendations();
        return ResponseEntity.ok(recommendations);
    }

    @Operation(summary = "Reset all cache statistics and baseline")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cache statistics reset successfully")
    })
    @DeleteMapping("/reset")
    public ResponseEntity<String> resetCacheStats() {
        String result = cacheAnalysisService.resetCacheStats();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get cache performance summary")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Performance summary retrieved")
    })
    @GetMapping("/summary")
    public ResponseEntity<String> getPerformanceSummary() {
        String summary = cacheAnalysisService.getPerformanceSummary();
        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Get individual cache analysis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cache analysis retrieved"),
            @ApiResponse(responseCode = "404", description = "Cache not found")
    })
    @GetMapping("/{cacheName}")
    public ResponseEntity<CacheComparison> getCacheAnalysis(@PathVariable String cacheName) {
        return cacheAnalysisService.getCacheAnalysis(cacheName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
