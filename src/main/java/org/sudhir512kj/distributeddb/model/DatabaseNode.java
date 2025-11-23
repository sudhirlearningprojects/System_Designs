package org.sudhir512kj.distributeddb.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseNode {
    private String nodeId;
    private String host;
    private int port;
    private NodeRole role;
    private NodeStatus status;
    private String shardId;
    private String region;
    private long replicationLag;
    private LocalDateTime lastHeartbeat;
    private int connectionPoolSize;
    private double cpuUsage;
    private double memoryUsage;

    public enum NodeRole {
        PRIMARY, REPLICA, COORDINATOR
    }

    public enum NodeStatus {
        HEALTHY, DEGRADED, OFFLINE, SYNCING
    }
}
