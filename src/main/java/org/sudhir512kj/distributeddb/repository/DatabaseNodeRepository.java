package org.sudhir512kj.distributeddb.repository;

import org.springframework.stereotype.Repository;
import org.sudhir512kj.distributeddb.model.DatabaseNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DatabaseNodeRepository {
    private final Map<String, DatabaseNode> nodes = new ConcurrentHashMap<>();

    public void save(DatabaseNode node) {
        nodes.put(node.getNodeId(), node);
    }

    public DatabaseNode findById(String nodeId) {
        return nodes.get(nodeId);
    }

    public Map<String, DatabaseNode> findAll() {
        return new ConcurrentHashMap<>(nodes);
    }
}
