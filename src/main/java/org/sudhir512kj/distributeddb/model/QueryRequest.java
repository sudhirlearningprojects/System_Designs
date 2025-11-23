package org.sudhir512kj.distributeddb.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {
    private String queryId;
    private String sql;
    private Map<String, Object> parameters;
    private QueryType type;
    private String shardingKey;
    private String tenantId;
    private String region;
    private ConsistencyLevel consistencyLevel;
    private long timeoutMs;

    public enum QueryType {
        SELECT, INSERT, UPDATE, DELETE, TRANSACTION
    }

    public enum ConsistencyLevel {
        STRONG, EVENTUAL, CAUSAL
    }
}
