package org.sudhir512kj.cloudinfra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.sudhir512kj.cloudinfra.model.ResourceMetric;
import org.sudhir512kj.cloudinfra.repository.ResourceMetricRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsCollectorService {
    private final ResourceMetricRepository metricRepository;
    
    @Async
    public void ingestMetrics(List<ResourceMetric> metrics) {
        metricRepository.saveAll(metrics);
        log.debug("Ingested {} metrics", metrics.size());
    }
    
    public List<ResourceMetric> getMetrics(String resourceId, LocalDateTime start, LocalDateTime end) {
        return metricRepository.findByResourceIdAndTimestampBetween(resourceId, start, end);
    }
    
    public void recordMetric(String resourceId, double cpu, double memory, double disk) {
        ResourceMetric metric = new ResourceMetric();
        metric.setResourceId(resourceId);
        metric.setTimestamp(LocalDateTime.now());
        metric.setCpuUsage(cpu);
        metric.setMemoryUsage(memory);
        metric.setDiskUsage(disk);
        
        metricRepository.save(metric);
    }
}
