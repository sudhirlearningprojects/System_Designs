package org.sudhir512kj.distributeddb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.sudhir512kj.distributeddb.model.QueryRequest;
import org.sudhir512kj.distributeddb.model.ShardConfig;
import org.sudhir512kj.distributeddb.router.ShardRegistry;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplicationService {
    private final ShardRegistry shardRegistry;
    private final ShardExecutor shardExecutor;

    @Async
    public void replicateAsync(String shardId, QueryRequest request) {
        ShardConfig shard = shardRegistry.getShard(shardId);
        
        if (shard.getReplicaNodes() == null || shard.getReplicaNodes().isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> futures = shard.getReplicaNodes().stream()
                .map(replica -> CompletableFuture.runAsync(() -> 
                        replicateToNode(replica, request)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("Replication completed for shard: {}", shardId))
                .exceptionally(ex -> {
                    log.error("Replication failed for shard {}: {}", shardId, ex.getMessage());
                    return null;
                });
    }

    private void replicateToNode(String nodeId, QueryRequest request) {
        try {
            log.debug("Replicating to node: {}", nodeId);
        } catch (Exception e) {
            log.error("Replication to node {} failed: {}", nodeId, e.getMessage());
            throw new RuntimeException("Replication failed", e);
        }
    }

    public void syncReplica(String shardId, String replicaNode) {
        log.info("Starting replica sync for shard {} on node {}", shardId, replicaNode);
    }
}
