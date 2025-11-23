package org.sudhir512kj.distributeddb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.distributeddb.model.ShardConfig;
import org.sudhir512kj.distributeddb.repository.ShardConfigRepository;
import org.sudhir512kj.distributeddb.router.ShardRegistry;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShardManagementService {
    private final ShardConfigRepository repository;
    private final ShardRegistry shardRegistry;

    public void registerShard(ShardConfig config) {
        config.setCreatedAt(LocalDateTime.now());
        config.setLastHealthCheck(LocalDateTime.now());
        config.setStatus(ShardConfig.ShardStatus.ACTIVE);
        
        repository.save(config);
        shardRegistry.registerShard(config);
        
        log.info("Shard registered: {}", config.getShardId());
    }

    public ShardConfig getShard(String shardId) {
        return repository.findById(shardId);
    }

    public List<ShardConfig> getAllShards() {
        return repository.findAll().values().stream().toList();
    }

    public void updateShardStatus(String shardId, ShardConfig.ShardStatus status) {
        ShardConfig config = repository.findById(shardId);
        if (config != null) {
            config.setStatus(status);
            config.setLastHealthCheck(LocalDateTime.now());
            repository.save(config);
            shardRegistry.updateShardStatus(shardId, status);
        }
    }

    public void removeShard(String shardId) {
        repository.delete(shardId);
        log.info("Shard removed: {}", shardId);
    }
}
