package org.sudhir512kj.connectionpool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sudhir512kj.connectionpool.metrics.PoolMetrics;
import org.sudhir512kj.connectionpool.model.PoolConfig;
import org.sudhir512kj.connectionpool.pool.ConnectionFactory;
import org.sudhir512kj.connectionpool.pool.ConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class ConnectionPoolService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPoolService.class);

    private final ConnectionPool pool;

    public ConnectionPoolService(PoolConfig config, ConnectionFactory factory) {
        this.pool = new ConnectionPool(config, factory);
    }

    public Connection getConnection() throws SQLException, TimeoutException {
        return pool.acquire();
    }

    public Connection getConnection(Duration timeout) throws SQLException, TimeoutException {
        return pool.acquire(timeout);
    }

    public <T> T execute(Function<Connection, T> action) throws SQLException {
        try (Connection conn = getConnection()) {
            return action.apply(conn);
        } catch (TimeoutException e) {
            throw new SQLException("Failed to acquire connection", e);
        }
    }

    public void executeVoid(java.util.function.Consumer<Connection> action) throws SQLException {
        try (Connection conn = getConnection()) {
            action.accept(conn);
        } catch (TimeoutException e) {
            throw new SQLException("Failed to acquire connection", e);
        }
    }

    public PoolMetrics getMetrics() { return pool.getMetrics(); }
    public int getActiveCount() { return pool.getActiveCount(); }
    public int getIdleCount() { return pool.getIdleCount(); }
    public int getTotalCount() { return pool.getTotalCount(); }
    public int getWaitingCount() { return pool.getWaitingCount(); }

    public PoolStatus getStatus() {
        return new PoolStatus(
            pool.getActiveCount(),
            pool.getIdleCount(),
            pool.getTotalCount(),
            pool.getWaitingCount(),
            pool.isClosed(),
            pool.getMetrics()
        );
    }

    @Override
    public void close() {
        pool.close();
    }

    public record PoolStatus(
        int active,
        int idle,
        int total,
        int waiting,
        boolean closed,
        PoolMetrics metrics
    ) {}
}
