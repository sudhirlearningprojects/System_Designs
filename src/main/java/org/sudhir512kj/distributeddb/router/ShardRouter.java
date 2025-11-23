package org.sudhir512kj.distributeddb.router;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sudhir512kj.distributeddb.model.QueryRequest;
import org.sudhir512kj.distributeddb.model.ShardConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShardRouter {
    private final ShardRegistry shardRegistry;

    public List<String> routeQuery(QueryRequest request) {
        List<ShardConfig> allShards = shardRegistry.getAllShards();

        if (request.getShardingKey() != null) {
            return List.of(routeByHash(request.getShardingKey(), allShards));
        }

        if (request.getTenantId() != null) {
            return routeByTenant(request.getTenantId(), allShards);
        }

        if (request.getRegion() != null) {
            return routeByGeo(request.getRegion(), allShards);
        }

        return allShards.stream()
                .filter(s -> s.getStatus() == ShardConfig.ShardStatus.ACTIVE)
                .map(ShardConfig::getShardId)
                .collect(Collectors.toList());
    }

    private String routeByHash(String key, List<ShardConfig> shards) {
        long hash = consistentHash(key);
        int index = (int) (Math.abs(hash) % shards.size());
        return shards.get(index).getShardId();
    }

    private List<String> routeByTenant(String tenantId, List<ShardConfig> shards) {
        return shards.stream()
                .filter(s -> s.getRange() != null && 
                       s.getRange().getTenantIds() != null &&
                       s.getRange().getTenantIds().contains(tenantId))
                .map(ShardConfig::getShardId)
                .collect(Collectors.toList());
    }

    private List<String> routeByGeo(String region, List<ShardConfig> shards) {
        return shards.stream()
                .filter(s -> region.equals(s.getRegion()))
                .map(ShardConfig::getShardId)
                .collect(Collectors.toList());
    }

    private long consistentHash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(key.getBytes(StandardCharsets.UTF_8));
            return ((long) (hash[0] & 0xFF) << 24) |
                   ((long) (hash[1] & 0xFF) << 16) |
                   ((long) (hash[2] & 0xFF) << 8) |
                   (hash[3] & 0xFF);
        } catch (Exception e) {
            return key.hashCode();
        }
    }
}
