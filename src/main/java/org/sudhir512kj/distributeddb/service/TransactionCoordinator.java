package org.sudhir512kj.distributeddb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.distributeddb.model.QueryRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionCoordinator {
    private final DistributedQueryService queryService;
    private final Map<String, TransactionState> transactions = new ConcurrentHashMap<>();

    public String beginTransaction() {
        String txnId = UUID.randomUUID().toString();
        transactions.put(txnId, new TransactionState());
        log.info("Transaction started: {}", txnId);
        return txnId;
    }

    public void executeInTransaction(String txnId, QueryRequest request) {
        TransactionState state = transactions.get(txnId);
        if (state == null) {
            throw new IllegalStateException("Transaction not found: " + txnId);
        }
        
        state.addQuery(request);
    }

    public boolean commit(String txnId) {
        TransactionState state = transactions.get(txnId);
        if (state == null) {
            throw new IllegalStateException("Transaction not found: " + txnId);
        }

        boolean prepareSuccess = preparePhase(txnId, state);
        if (!prepareSuccess) {
            rollback(txnId);
            return false;
        }

        commitPhase(txnId, state);
        transactions.remove(txnId);
        log.info("Transaction committed: {}", txnId);
        return true;
    }

    public void rollback(String txnId) {
        TransactionState state = transactions.remove(txnId);
        if (state != null) {
            log.info("Transaction rolled back: {}", txnId);
        }
    }

    private boolean preparePhase(String txnId, TransactionState state) {
        log.debug("Prepare phase for transaction: {}", txnId);
        return true;
    }

    private void commitPhase(String txnId, TransactionState state) {
        for (QueryRequest query : state.getQueries()) {
            queryService.executeQuery(query);
        }
    }

    private static class TransactionState {
        private final List<QueryRequest> queries = new ArrayList<>();

        public void addQuery(QueryRequest request) {
            queries.add(request);
        }

        public List<QueryRequest> getQueries() {
            return queries;
        }
    }
}
