package org.sudhir512kj.jobscheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
@Slf4j
public class LeaseManagerService {
    private final JdbcTemplate jdbcTemplate;
    
    @Value("${app.scheduler.lease.duration-seconds:30}")
    private long leaseDurationSeconds;
    
    @Value("${app.scheduler.lease.heartbeat-interval-seconds:10}")
    private long heartbeatIntervalSeconds;
    
    private final Map<String, Boolean> heldLeases = new ConcurrentHashMap<>();
    private String nodeId;
    
    public LeaseManagerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        try {
            this.nodeId = InetAddress.getLocalHost().getHostName() + "-" + System.currentTimeMillis();
        } catch (Exception e) {
            this.nodeId = "node-" + System.currentTimeMillis();
        }
        log.info("LeaseManager initialized with nodeId: {}", nodeId);
    }
    
    public boolean acquireLease(String partitionKey) {
        try {
            Instant now = Instant.now();
            Instant leaseExpiry = now.plusSeconds(leaseDurationSeconds);
            
            int updated = jdbcTemplate.update(
                "INSERT INTO scheduler_leases (partition_key, node_id, lease_expires_at, heartbeat_at) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (partition_key) DO UPDATE SET " +
                "node_id = EXCLUDED.node_id, " +
                "lease_expires_at = EXCLUDED.lease_expires_at, " +
                "heartbeat_at = EXCLUDED.heartbeat_at " +
                "WHERE scheduler_leases.lease_expires_at < NOW()",
                partitionKey, nodeId, 
                Timestamp.from(leaseExpiry),
                Timestamp.from(now)
            );
            
            if (updated > 0) {
                heldLeases.put(partitionKey, true);
                log.debug("Acquired lease for partition: {}", partitionKey);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Failed to acquire lease for partition: {}", partitionKey, e);
            return false;
        }
    }
    
    public boolean renewLease(String partitionKey) {
        try {
            Instant now = Instant.now();
            Instant leaseExpiry = now.plusSeconds(leaseDurationSeconds);
            
            int updated = jdbcTemplate.update(
                "UPDATE scheduler_leases SET " +
                "lease_expires_at = ?, heartbeat_at = ? " +
                "WHERE partition_key = ? AND node_id = ?",
                Timestamp.from(leaseExpiry),
                Timestamp.from(now),
                partitionKey, nodeId
            );
            
            if (updated > 0) {
                log.debug("Renewed lease for partition: {}", partitionKey);
                return true;
            } else {
                heldLeases.remove(partitionKey);
                log.warn("Failed to renew lease for partition: {} - lease may have been taken by another node", partitionKey);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to renew lease for partition: {}", partitionKey, e);
            heldLeases.remove(partitionKey);
            return false;
        }
    }
    
    public void releaseLease(String partitionKey) {
        try {
            jdbcTemplate.update(
                "DELETE FROM scheduler_leases WHERE partition_key = ? AND node_id = ?",
                partitionKey, nodeId
            );
            
            heldLeases.remove(partitionKey);
            log.info("Released lease for partition: {}", partitionKey);
        } catch (Exception e) {
            log.error("Failed to release lease for partition: {}", partitionKey, e);
        }
    }
    
    public boolean hasLease(String partitionKey) {
        return heldLeases.getOrDefault(partitionKey, false);
    }
    
    @Scheduled(fixedDelayString = "${app.scheduler.lease.heartbeat-interval-seconds:10}000")
    public void maintainLeases() {
        for (String partitionKey : heldLeases.keySet()) {
            if (!renewLease(partitionKey)) {
                log.warn("Lost lease for partition: {}", partitionKey);
            }
        }
        
        // Try to acquire the main scheduler lease if we don't have it
        if (!hasLease("scheduler-main")) {
            acquireLease("scheduler-main");
        }
    }
    
    @Scheduled(fixedDelay = 60000) // Every minute
    public void cleanupExpiredLeases() {
        try {
            int deleted = jdbcTemplate.update(
                "DELETE FROM scheduler_leases WHERE lease_expires_at < NOW()"
            );
            
            if (deleted > 0) {
                log.info("Cleaned up {} expired leases", deleted);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup expired leases", e);
        }
    }
    
    public String getNodeId() {
        return nodeId;
    }
}