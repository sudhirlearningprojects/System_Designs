package org.sudhir512kj.connectionpool.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sudhir512kj.connectionpool.metrics.PoolMetrics;
import org.sudhir512kj.connectionpool.model.PoolConfig;
import org.sudhir512kj.connectionpool.model.PooledConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionPool implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);

    private final PoolConfig config;
    private final ConnectionFactory factory;
    private final PoolMetrics metrics;

    private final ConcurrentLinkedDeque<PooledConnection> idleQueue = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<Connection, PooledConnection> activeConnections = new ConcurrentHashMap<>();
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final Semaphore semaphore;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicInteger waitingThreads = new AtomicInteger(0);

    private final ScheduledExecutorService scheduler;

    public ConnectionPool(PoolConfig config, ConnectionFactory factory) {
        this.config = config;
        this.factory = factory;
        this.metrics = new PoolMetrics();
        this.semaphore = new Semaphore(config.getMaxTotal(), true); // fair

        this.scheduler = Executors.newScheduledThreadPool(3, r -> {
            Thread t = new Thread(r, config.getPoolName() + "-maintenance");
            t.setDaemon(true);
            return t;
        });

        warmUp();
        startMaintenanceTasks();
        log.info("[{}] Pool initialized: minIdle={}, maxTotal={}", config.getPoolName(), config.getMinIdle(),
                config.getMaxTotal());
    }

    // === Public API ===

    public Connection acquire() throws SQLException, TimeoutException {
        return acquire(config.getMaxWait());
    }

    public Connection acquire(Duration timeout) throws SQLException, TimeoutException {
        checkNotClosed();
        long startNanos = System.nanoTime();

        try {
            waitingThreads.incrementAndGet();

            if (!semaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                metrics.recordTimeout();
                throw new TimeoutException(String.format(
                        "[%s] Timed out after %dms waiting for connection. Active=%d, Idle=%d, Total=%d, Waiting=%d",
                        config.getPoolName(), timeout.toMillis(),
                        activeConnections.size(), idleQueue.size(), totalConnections.get(), waitingThreads.get()));
            }

            PooledConnection pooled = tryAcquireFromIdle();
            if (pooled != null) {
                long elapsed = System.nanoTime() - startNanos;
                metrics.recordAcquire(elapsed);
                metrics.updatePeakActive(activeConnections.size());
                return new ConnectionProxy(pooled.getRealConnection(), this);
            }

            // Create new connection
            pooled = createNewConnection();
            long elapsed = System.nanoTime() - startNanos;
            metrics.recordAcquire(elapsed);
            metrics.updatePeakActive(activeConnections.size());
            return new ConnectionProxy(pooled.getRealConnection(), this);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for connection", e);
        } finally {
            waitingThreads.decrementAndGet();
        }
    }

    public void release(Connection realConnection) {
        if (closed.get()) {
            destroyConnection(realConnection);
            return;
        }

        PooledConnection pooled = activeConnections.remove(realConnection);
        if (pooled == null) {
            log.warn("[{}] Attempted to release unknown connection", config.getPoolName());
            return;
        }

        pooled.markIdle();
        metrics.recordRelease();

        // Validate if configured
        if (config.isTestOnReturn() && !validate(pooled)) {
            destroyPooledConnection(pooled);
            semaphore.release();
            replenishIfNeeded();
            return;
        }

        // Check if expired
        if (pooled.isExpired(config.getMaxLifetime())) {
            destroyPooledConnection(pooled);
            semaphore.release();
            replenishIfNeeded();
            return;
        }

        // Return to idle queue
        if (idleQueue.size() < config.getMaxIdle()) {
            idleQueue.offerFirst(pooled);
        } else {
            destroyPooledConnection(pooled);
        }
        semaphore.release();
    }

    // === Pool State ===

    public int getActiveCount() {
        return activeConnections.size();
    }

    public int getIdleCount() {
        return idleQueue.size();
    }

    public int getTotalCount() {
        return totalConnections.get();
    }

    public int getWaitingCount() {
        return waitingThreads.get();
    }

    public PoolMetrics getMetrics() {
        return metrics;
    }

    public boolean isClosed() {
        return closed.get();
    }

    // === Shutdown ===

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true))
            return;

        log.info("[{}] Shutting down pool...", config.getPoolName());
        scheduler.shutdownNow();

        // Destroy idle connections
        PooledConnection pooled;
        while ((pooled = idleQueue.poll()) != null) {
            destroyPooledConnection(pooled);
        }

        // Force-close active connections
        for (PooledConnection active : activeConnections.values()) {
            log.warn("[{}] Force-closing active connection held by: {}", config.getPoolName(), active.getOwnerThread());
            destroyPooledConnection(active);
        }
        activeConnections.clear();

        log.info("[{}] Pool shut down. Final metrics: {}", config.getPoolName(), metrics);
    }

    // === Internal Methods ===

    private void warmUp() {
        for (int i = 0; i < config.getMinIdle(); i++) {
            try {
                PooledConnection conn = createPooledConnection();
                idleQueue.offerLast(conn);
            } catch (SQLException e) {
                log.warn("[{}] Failed to create connection during warm-up: {}", config.getPoolName(), e.getMessage());
            }
        }
    }

    private PooledConnection tryAcquireFromIdle() {
        PooledConnection pooled;
        while ((pooled = idleQueue.pollFirst()) != null) {
            if (pooled.isExpired(config.getMaxLifetime())) {
                destroyPooledConnection(pooled);
                continue;
            }
            if (config.isTestOnBorrow() && !validate(pooled)) {
                destroyPooledConnection(pooled);
                metrics.recordValidationFailure();
                continue;
            }
            if (pooled.markInUse()) {
                activeConnections.put(pooled.getRealConnection(), pooled);
                return pooled;
            }
        }
        return null;
    }

    private PooledConnection createNewConnection() throws SQLException {
        PooledConnection pooled = createPooledConnection();
        pooled.markInUse();
        activeConnections.put(pooled.getRealConnection(), pooled);
        return pooled;
    }

    private PooledConnection createPooledConnection() throws SQLException {
        Connection real = factory.createConnection();
        totalConnections.incrementAndGet();
        metrics.recordCreation();
        return new PooledConnection(real);
    }

    private void destroyPooledConnection(PooledConnection pooled) {
        if (pooled.markClosed()) {
            factory.destroyConnection(pooled.getRealConnection());
            totalConnections.decrementAndGet();
            metrics.recordDestruction();
        }
    }

    private void destroyConnection(Connection conn) {
        factory.destroyConnection(conn);
        totalConnections.decrementAndGet();
        metrics.recordDestruction();
    }

    private boolean validate(PooledConnection pooled) {
        boolean valid = factory.validateConnection(pooled.getRealConnection(), config.getValidationQuery());
        if (valid)
            pooled.updateValidation();
        return valid;
    }

    private void replenishIfNeeded() {
        while (totalConnections.get() < config.getMinIdle() && !closed.get()) {
            try {
                PooledConnection conn = createPooledConnection();
                idleQueue.offerLast(conn);
            } catch (SQLException e) {
                log.warn("[{}] Failed to replenish pool: {}", config.getPoolName(), e.getMessage());
                break;
            }
        }
    }

    private void checkNotClosed() throws SQLException {
        if (closed.get())
            throw new SQLException("Pool is closed");
    }

    // === Maintenance Tasks ===

    private void startMaintenanceTasks() {
        // Idle eviction
        scheduler.scheduleWithFixedDelay(this::evictIdleConnections,
                config.getTimeBetweenEvictionRuns().toMillis(),
                config.getTimeBetweenEvictionRuns().toMillis(),
                TimeUnit.MILLISECONDS);

        // Health check
        if (config.isTestWhileIdle()) {
            scheduler.scheduleWithFixedDelay(this::validateIdleConnections,
                    config.getValidationInterval().toMillis(),
                    config.getValidationInterval().toMillis(),
                    TimeUnit.MILLISECONDS);
        }

        // Leak detection
        if (config.getLeakDetectionThreshold().toMillis() > 0) {
            scheduler.scheduleWithFixedDelay(this::detectLeaks,
                    30_000, 30_000, TimeUnit.MILLISECONDS);
        }
    }

    private void evictIdleConnections() {
        if (closed.get())
            return;

        List<PooledConnection> toEvict = new ArrayList<>();
        for (PooledConnection pooled : idleQueue) {
            if (pooled.isExpired(config.getMaxLifetime())) {
                toEvict.add(pooled);
            } else if (idleQueue.size() > config.getMinIdle() &&
                    pooled.isIdleTooLong(config.getMinEvictableIdleTime())) {
                toEvict.add(pooled);
            }
        }

        for (PooledConnection pooled : toEvict) {
            if (idleQueue.remove(pooled)) {
                destroyPooledConnection(pooled);
                log.debug("[{}] Evicted idle connection (age={}ms)", config.getPoolName(),
                        Duration.between(pooled.getCreatedAt(), Instant.now()).toMillis());
            }
        }

        replenishIfNeeded();
    }

    private void validateIdleConnections() {
        if (closed.get())
            return;

        List<PooledConnection> invalid = new ArrayList<>();
        for (PooledConnection pooled : idleQueue) {
            if (Duration.between(pooled.getLastValidatedAt(), Instant.now())
                    .compareTo(config.getValidationInterval()) > 0) {
                if (!validate(pooled)) {
                    invalid.add(pooled);
                }
            }
        }

        for (PooledConnection pooled : invalid) {
            if (idleQueue.remove(pooled)) {
                destroyPooledConnection(pooled);
                metrics.recordValidationFailure();
            }
        }

        replenishIfNeeded();
    }

    private void detectLeaks() {
        if (closed.get())
            return;

        Instant threshold = Instant.now().minus(config.getLeakDetectionThreshold());
        for (PooledConnection pooled : activeConnections.values()) {
            Instant acquired = pooled.getAcquiredAt();
            if (acquired != null && acquired.isBefore(threshold)) {
                metrics.recordLeak();
                log.warn("[{}] POTENTIAL CONNECTION LEAK! Connection held by thread '{}' for {}ms (threshold={}ms)",
                        config.getPoolName(),
                        pooled.getOwnerThread(),
                        Duration.between(acquired, Instant.now()).toMillis(),
                        config.getLeakDetectionThreshold().toMillis());
            }
        }
    }
}
