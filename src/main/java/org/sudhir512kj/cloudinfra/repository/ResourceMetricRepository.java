package org.sudhir512kj.cloudinfra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.cloudinfra.model.ResourceMetric;

import java.time.LocalDateTime;
import java.util.List;

public interface ResourceMetricRepository extends JpaRepository<ResourceMetric, Long> {
    List<ResourceMetric> findByResourceIdAndTimestampBetween(String resourceId, LocalDateTime start, LocalDateTime end);
}
