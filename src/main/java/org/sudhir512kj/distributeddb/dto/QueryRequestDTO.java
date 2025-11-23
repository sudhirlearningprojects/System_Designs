package org.sudhir512kj.distributeddb.dto;

import lombok.Data;
import org.sudhir512kj.distributeddb.model.QueryRequest;

import java.util.Map;

@Data
public class QueryRequestDTO {
    private String queryId;
    private String sql;
    private Map<String, Object> parameters;
    private QueryRequest.QueryType type;
    private String shardingKey;
    private String tenantId;
    private String region;
    private QueryRequest.ConsistencyLevel consistencyLevel;
    private long timeoutMs;
}
