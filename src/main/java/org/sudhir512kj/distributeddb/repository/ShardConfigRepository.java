package org.sudhir512kj.distributeddb.repository;

import org.springframework.stereotype.Repository;
import org.sudhir512kj.distributeddb.model.ShardConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ShardConfigRepository {
    private final Map<String, ShardConfig> shards = new ConcurrentHashMap<>();

    public void save(ShardConfig config) {
        shards.put(config.getShardId(), config);
    }

    public ShardConfig findById(String shardId) {
        return shards.get(shardId);
    }

    public Map<String, ShardConfig> findAll() {
        return new ConcurrentHashMap<>(shards);
    }

    public void delete(String shardId) {
        shards.remove(shardId);
    }
}
