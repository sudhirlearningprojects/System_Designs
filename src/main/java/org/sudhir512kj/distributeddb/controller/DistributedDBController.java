package org.sudhir512kj.distributeddb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.distributeddb.dto.QueryRequestDTO;
import org.sudhir512kj.distributeddb.dto.ShardConfigDTO;
import org.sudhir512kj.distributeddb.model.QueryRequest;
import org.sudhir512kj.distributeddb.model.ShardConfig;
import org.sudhir512kj.distributeddb.service.DistributedQueryService;
import org.sudhir512kj.distributeddb.service.ShardManagementService;
import org.sudhir512kj.distributeddb.service.TransactionCoordinator;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/db")
@RequiredArgsConstructor
public class DistributedDBController {
    private final DistributedQueryService queryService;
    private final TransactionCoordinator transactionCoordinator;
    private final ShardManagementService shardManagementService;

    @PostMapping("/query")
    public ResponseEntity<List<Map<String, Object>>> executeQuery(@RequestBody QueryRequestDTO dto) {
        QueryRequest request = mapToQueryRequest(dto);
        List<Map<String, Object>> result = queryService.executeQuery(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/transaction/begin")
    public ResponseEntity<Map<String, String>> beginTransaction() {
        String txnId = transactionCoordinator.beginTransaction();
        return ResponseEntity.ok(Map.of("transactionId", txnId));
    }

    @PostMapping("/transaction/{txnId}/execute")
    public ResponseEntity<Void> executeInTransaction(
            @PathVariable String txnId,
            @RequestBody QueryRequestDTO dto) {
        QueryRequest request = mapToQueryRequest(dto);
        transactionCoordinator.executeInTransaction(txnId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transaction/{txnId}/commit")
    public ResponseEntity<Map<String, Boolean>> commitTransaction(@PathVariable String txnId) {
        boolean success = transactionCoordinator.commit(txnId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping("/transaction/{txnId}/rollback")
    public ResponseEntity<Void> rollbackTransaction(@PathVariable String txnId) {
        transactionCoordinator.rollback(txnId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/shards")
    public ResponseEntity<Void> registerShard(@RequestBody ShardConfigDTO dto) {
        ShardConfig config = mapToShardConfig(dto);
        shardManagementService.registerShard(config);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/shards")
    public ResponseEntity<List<ShardConfig>> getAllShards() {
        return ResponseEntity.ok(shardManagementService.getAllShards());
    }

    @GetMapping("/shards/{shardId}")
    public ResponseEntity<ShardConfig> getShard(@PathVariable String shardId) {
        return ResponseEntity.ok(shardManagementService.getShard(shardId));
    }

    private QueryRequest mapToQueryRequest(QueryRequestDTO dto) {
        return QueryRequest.builder()
                .queryId(dto.getQueryId())
                .sql(dto.getSql())
                .parameters(dto.getParameters())
                .type(dto.getType())
                .shardingKey(dto.getShardingKey())
                .tenantId(dto.getTenantId())
                .region(dto.getRegion())
                .consistencyLevel(dto.getConsistencyLevel())
                .timeoutMs(dto.getTimeoutMs())
                .build();
    }

    private ShardConfig mapToShardConfig(ShardConfigDTO dto) {
        return ShardConfig.builder()
                .shardId(dto.getShardId())
                .type(dto.getType())
                .strategy(dto.getStrategy())
                .primaryNode(dto.getPrimaryNode())
                .replicaNodes(dto.getReplicaNodes())
                .region(dto.getRegion())
                .status(ShardConfig.ShardStatus.ACTIVE)
                .build();
    }
}
