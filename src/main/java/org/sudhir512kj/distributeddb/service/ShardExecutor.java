package org.sudhir512kj.distributeddb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.sudhir512kj.distributeddb.model.DatabaseNode;
import org.sudhir512kj.distributeddb.model.QueryRequest;
import org.sudhir512kj.distributeddb.model.ShardConfig;
import org.sudhir512kj.distributeddb.repository.DatabaseNodeRepository;
import org.sudhir512kj.distributeddb.router.ShardRegistry;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShardExecutor {
    private final ShardRegistry shardRegistry;
    private final DatabaseNodeRepository nodeRepository;
    private final ConnectionPoolManager poolManager;
    private final Map<String, JdbcTemplate> jdbcTemplates = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public List<Map<String, Object>> executeOnPrimary(String shardId, QueryRequest request) {
        ShardConfig shard = shardRegistry.getShard(shardId);
        if (shard == null) {
            throw new RuntimeException("Shard not found: " + shardId);
        }
        
        JdbcTemplate jdbc = getJdbcTemplate(shard.getPrimaryNode());
        return executeQuery(jdbc, request);
    }

    public List<Map<String, Object>> executeOnReplica(String shardId, QueryRequest request) {
        ShardConfig shard = shardRegistry.getShard(shardId);
        
        if (shard.getReplicaNodes() == null || shard.getReplicaNodes().isEmpty()) {
            return executeOnPrimary(shardId, request);
        }
        
        String replicaNode = shard.getReplicaNodes()
                .get(random.nextInt(shard.getReplicaNodes().size()));
        JdbcTemplate jdbc = getJdbcTemplate(replicaNode);
        
        return executeQuery(jdbc, request);
    }

    private List<Map<String, Object>> executeQuery(JdbcTemplate jdbc, QueryRequest request) {
        try {
            if (request.getType() == QueryRequest.QueryType.SELECT) {
                if (request.getParameters() == null || request.getParameters().isEmpty()) {
                    return jdbc.queryForList(request.getSql());
                }
                return jdbc.queryForList(request.getSql(), 
                        request.getParameters().values().toArray());
            } else {
                int rows = 0;
                if (request.getParameters() == null || request.getParameters().isEmpty()) {
                    rows = jdbc.update(request.getSql());
                } else {
                    rows = jdbc.update(request.getSql(), 
                            request.getParameters().values().toArray());
                }
                return List.of(Map.of("affected_rows", rows));
            }
        } catch (Exception e) {
            log.error("Query execution failed: {}", e.getMessage());
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        }
    }

    private JdbcTemplate getJdbcTemplate(String nodeId) {
        return jdbcTemplates.computeIfAbsent(nodeId, k -> {
            DatabaseNode node = nodeRepository.findById(nodeId);
            if (node == null) {
                node = DatabaseNode.builder()
                        .nodeId(nodeId)
                        .host("localhost")
                        .port(5432)
                        .build();
            }
            
            DataSource ds = poolManager.getOrCreatePool(
                    nodeId, node.getHost(), node.getPort(), "distributeddb");
            return new JdbcTemplate(ds);
        });
    }
}
