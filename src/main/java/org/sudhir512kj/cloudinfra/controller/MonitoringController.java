package org.sudhir512kj.cloudinfra.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.cloudinfra.model.ResourceMetric;
import org.sudhir512kj.cloudinfra.service.MetricsCollectorService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
public class MonitoringController {
    private final MetricsCollectorService metricsService;
    
    @GetMapping("/resources/{resourceId}/metrics")
    public ResponseEntity<List<ResourceMetric>> getMetrics(
            @PathVariable String resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(metricsService.getMetrics(resourceId, start, end));
    }
    
    @PostMapping("/metrics/ingest")
    public ResponseEntity<Void> ingestMetrics(@RequestBody List<ResourceMetric> metrics) {
        metricsService.ingestMetrics(metrics);
        return ResponseEntity.ok().build();
    }
}
