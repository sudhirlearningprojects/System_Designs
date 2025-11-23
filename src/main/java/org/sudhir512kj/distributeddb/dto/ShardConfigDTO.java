package org.sudhir512kj.distributeddb.dto;

import lombok.Data;
import org.sudhir512kj.distributeddb.model.ShardConfig;

import java.util.List;

@Data
public class ShardConfigDTO {
    private String shardId;
    private ShardConfig.ShardType type;
    private ShardConfig.ShardStrategy strategy;
    private String primaryNode;
    private List<String> replicaNodes;
    private String region;
}
