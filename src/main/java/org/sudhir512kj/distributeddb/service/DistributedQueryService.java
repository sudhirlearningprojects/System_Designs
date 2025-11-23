package org.sudhir512kj.distributeddb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.distributeddb.model.QueryRequest;
import org.sudhir512kj.distributeddb.router.ShardRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedQueryService {
    private final ShardRouter shardRouter;
    private final ShardExecutor shardExecutor;
    private final ReplicationService replicationService;

    public List<Map<String, Object>> executeQuery(QueryRequest request) {
        List<String> targetShards = shardRouter.routeQuery(request);
        
        if (targetShards.size() == 1) {
            return executeSingleShard(targetShards.get(0), request);
        }
        
        return executeMultiShard(targetShards, request);
    }

    private List<Map<String, Object>> executeSingleShard(String shardId, QueryRequest request) {
        try {
            if (request.getType() == QueryRequest.QueryType.SELECT &&
                request.getConsistencyLevel() == QueryRequest.ConsistencyLevel.EVENTUAL) {
                return shardExecutor.executeOnReplica(shardId, request);
            }
            
            List<Map<String, Object>> result = shardExecutor.executeOnPrimary(shardId, request);
            
            if (isWriteOperation(request.getType())) {
                replicationService.replicateAsync(shardId, request);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Query execution failed on shard {}: {}", shardId, e.getMessage());
            throw new RuntimeException("Query execution failed", e);
        }
    }

    private List<Map<String, Object>> executeMultiShard(List<String> shardIds, QueryRequest request) {
        List<CompletableFuture<List<Map<String, Object>>>> futures = new ArrayList<>();
        
        for (String shardId : shardIds) {
            CompletableFuture<List<Map<String, Object>>> future = 
                CompletableFuture.supplyAsync(() -> executeSingleShard(shardId, request));
            futures.add(future);
        }
        
        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
    }

    private boolean isWriteOperation(QueryRequest.QueryType type) {
        return type == QueryRequest.QueryType.INSERT ||
               type == QueryRequest.QueryType.UPDATE ||
               type == QueryRequest.QueryType.DELETE;
    }
}
