package org.sudhir512kj.distributeddb.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConnectionPoolManager {
    private final Map<String, DataSource> pools = new ConcurrentHashMap<>();

    public DataSource getOrCreatePool(String nodeId, String host, int port, String database) {
        return pools.computeIfAbsent(nodeId, k -> createPool(host, port, database));
    }

    private DataSource createPool(String host, int port, String database) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        config.setUsername("postgres");
        config.setPassword("password");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        
        log.info("Created connection pool for {}:{}/{}", host, port, database);
        return new HikariDataSource(config);
    }

    public void closePool(String nodeId) {
        DataSource ds = pools.remove(nodeId);
        if (ds instanceof HikariDataSource) {
            ((HikariDataSource) ds).close();
            log.info("Closed connection pool for node: {}", nodeId);
        }
    }
}
