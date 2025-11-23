package org.sudhir512kj.distributeddb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.sudhir512kj.distributeddb.model.ShardConfig;
import org.sudhir512kj.distributeddb.router.ShardRegistry;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FailoverService {
    private final ShardRegistry shardRegistry;
    private final ReplicationService replicationService;

    @Scheduled(fixedRate = 5000)
    public void healthCheck() {
        List<ShardConfig> shards = shardRegistry.getAllShards();
        
        for (ShardConfig shard : shards) {
            if (!isPrimaryHealthy(shard)) {
                log.warn("Primary unhealthy for shard: {}", shard.getShardId());
                performFailover(shard);
            }
        }
    }

    private boolean isPrimaryHealthy(ShardConfig shard) {
        if (shard.getLastHealthCheck() == null) {
            return true;
        }
        return shard.getLastHealthCheck().isAfter(LocalDateTime.now().minusSeconds(30));
    }

    private void performFailover(ShardConfig shard) {
        if (shard.getReplicaNodes() == null || shard.getReplicaNodes().isEmpty()) {
            log.error("No replicas available for failover: {}", shard.getShardId());
            shardRegistry.updateShardStatus(shard.getShardId(), ShardConfig.ShardStatus.OFFLINE);
            return;
        }

        String newPrimary = shard.getReplicaNodes().get(0);
        log.info("Promoting replica {} to primary for shard {}", newPrimary, shard.getShardId());
        
        String oldPrimary = shard.getPrimaryNode();
        shard.setPrimaryNode(newPrimary);
        shard.getReplicaNodes().remove(newPrimary);
        shard.getReplicaNodes().add(oldPrimary);
        
        shardRegistry.updateShardStatus(shard.getShardId(), ShardConfig.ShardStatus.ACTIVE);
        
        replicationService.syncReplica(shard.getShardId(), newPrimary);
    }
}
