package org.sudhir512kj.connectionpool.model;

import java.sql.Connection;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class PooledConnection {

    public enum State { IDLE, IN_USE, EVICTED, CLOSED }

    private final Connection realConnection;
    private final Instant createdAt;
    private final AtomicReference<State> state;
    private final AtomicLong borrowCount;

    private volatile Instant lastUsedAt;
    private volatile Instant lastValidatedAt;
    private volatile Instant acquiredAt;
    private volatile String ownerThread;

    public PooledConnection(Connection realConnection) {
        this.realConnection = realConnection;
        this.createdAt = Instant.now();
        this.lastUsedAt = Instant.now();
        this.lastValidatedAt = Instant.now();
        this.state = new AtomicReference<>(State.IDLE);
        this.borrowCount = new AtomicLong(0);
    }

    public Connection getRealConnection() { return realConnection; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public Instant getLastValidatedAt() { return lastValidatedAt; }
    public Instant getAcquiredAt() { return acquiredAt; }
    public String getOwnerThread() { return ownerThread; }
    public long getBorrowCount() { return borrowCount.get(); }
    public State getState() { return state.get(); }

    public boolean markInUse() {
        if (state.compareAndSet(State.IDLE, State.IN_USE)) {
            this.acquiredAt = Instant.now();
            this.ownerThread = Thread.currentThread().getName();
            this.borrowCount.incrementAndGet();
            return true;
        }
        return false;
    }

    public boolean markIdle() {
        if (state.compareAndSet(State.IN_USE, State.IDLE)) {
            this.lastUsedAt = Instant.now();
            this.ownerThread = null;
            this.acquiredAt = null;
            return true;
        }
        return false;
    }

    public boolean markEvicted() {
        State current = state.get();
        return current != State.CLOSED && state.compareAndSet(current, State.EVICTED);
    }

    public boolean markClosed() {
        State current = state.get();
        return state.compareAndSet(current, State.CLOSED);
    }

    public void updateValidation() {
        this.lastValidatedAt = Instant.now();
    }

    public boolean isExpired(java.time.Duration maxLifetime) {
        return Instant.now().isAfter(createdAt.plus(maxLifetime));
    }

    public boolean isIdleTooLong(java.time.Duration idleTimeout) {
        return state.get() == State.IDLE &&
               Instant.now().isAfter(lastUsedAt.plus(idleTimeout));
    }

    @Override
    public String toString() {
        return String.format("PooledConnection[state=%s, age=%dms, borrows=%d, owner=%s]",
                state.get(),
                java.time.Duration.between(createdAt, Instant.now()).toMillis(),
                borrowCount.get(),
                ownerThread);
    }
}
