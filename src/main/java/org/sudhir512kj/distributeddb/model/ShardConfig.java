package org.sudhir512kj.distributeddb.model;

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
public class ShardConfig {
    private String shardId;
    private ShardType type;
    private ShardStrategy strategy;
    private String primaryNode;
    private List<String> replicaNodes;
    private ShardRange range;
    private String region;
    private ShardStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastHealthCheck;

    public enum ShardType {
        HASH, RANGE, GEO, COMPOSITE
    }

    public enum ShardStrategy {
        CONSISTENT_HASH, RANGE_BASED, GEO_PROXIMITY, TENANT_BASED
    }

    public enum ShardStatus {
        ACTIVE, DEGRADED, OFFLINE, REBALANCING
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShardRange {
        private Object startKey;
        private Object endKey;
        private String geoRegion;
        private List<String> tenantIds;
    }
}
