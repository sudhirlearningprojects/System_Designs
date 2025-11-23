package org.sudhir512kj.distributeddb.router;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sudhir512kj.distributeddb.model.ShardConfig;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ShardRegistry {
    private final Map<String, ShardConfig> shards = new ConcurrentHashMap<>();

    public void registerShard(ShardConfig config) {
        shards.put(config.getShardId(), config);
        log.info("Registered shard: {}", config.getShardId());
    }

    public ShardConfig getShard(String shardId) {
        return shards.get(shardId);
    }

    public List<ShardConfig> getAllShards() {
        return new ArrayList<>(shards.values());
    }

    public void updateShardStatus(String shardId, ShardConfig.ShardStatus status) {
        ShardConfig shard = shards.get(shardId);
        if (shard != null) {
            shard.setStatus(status);
            shard.setLastHealthCheck(LocalDateTime.now());
        }
    }

    public List<ShardConfig> getHealthyShards() {
        return shards.values().stream()
                .filter(s -> s.getStatus() == ShardConfig.ShardStatus.ACTIVE)
                .toList();
    }
}
