package org.sudhir512kj.connectionpool.health;

import org.sudhir512kj.connectionpool.service.ConnectionPoolService;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConnectionPoolHealthIndicator {

    private final ConnectionPoolService poolService;
    private final double unhealthyThreshold;

    public ConnectionPoolHealthIndicator(ConnectionPoolService poolService) {
        this(poolService, 0.9); // 90% utilization = unhealthy
    }

    public ConnectionPoolHealthIndicator(ConnectionPoolService poolService, double unhealthyThreshold) {
        this.poolService = poolService;
        this.unhealthyThreshold = unhealthyThreshold;
    }

    public HealthStatus check() {
        var status = poolService.getStatus();

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("active", status.active());
        details.put("idle", status.idle());
        details.put("total", status.total());
        details.put("waiting", status.waiting());
        details.put("avgAcquireMs", String.format("%.2f", status.metrics().getAverageAcquireTimeMs()));
        details.put("totalTimeouts", status.metrics().getTotalTimeouts());
        details.put("totalLeaks", status.metrics().getTotalLeaksDetected());

        if (status.closed()) {
            return new HealthStatus("DOWN", "Pool is closed", details);
        }

        double utilization = status.total() > 0 ? (double) status.active() / status.total() : 0;
        details.put("utilization", String.format("%.1f%%", utilization * 100));

        if (utilization > unhealthyThreshold) {
            return new HealthStatus("DEGRADED", "Pool utilization above threshold", details);
        }

        if (status.waiting() > 0) {
            return new HealthStatus("DEGRADED", "Threads waiting for connections", details);
        }

        return new HealthStatus("UP", "Pool is healthy", details);
    }

    public record HealthStatus(String status, String message, Map<String, Object> details) {}
}
