package org.sudhir512kj.connectionpool.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class PoolMetrics {

    private final LongAdder totalAcquired = new LongAdder();
    private final LongAdder totalReleased = new LongAdder();
    private final LongAdder totalCreated = new LongAdder();
    private final LongAdder totalDestroyed = new LongAdder();
    private final LongAdder totalTimeouts = new LongAdder();
    private final LongAdder totalLeaksDetected = new LongAdder();
    private final LongAdder totalValidationFailures = new LongAdder();
    private final AtomicLong peakActiveConnections = new AtomicLong(0);
    private final AtomicLong totalAcquireTimeNanos = new AtomicLong(0);
    private final AtomicLong maxAcquireTimeNanos = new AtomicLong(0);

    public void recordAcquire(long acquireTimeNanos) {
        totalAcquired.increment();
        totalAcquireTimeNanos.addAndGet(acquireTimeNanos);
        long current;
        do {
            current = maxAcquireTimeNanos.get();
            if (acquireTimeNanos <= current)
                break;
        } while (!maxAcquireTimeNanos.compareAndSet(current, acquireTimeNanos));
    }

    public void recordRelease() {
        totalReleased.increment();
    }

    public void recordCreation() {
        totalCreated.increment();
    }

    public void recordDestruction() {
        totalDestroyed.increment();
    }

    public void recordTimeout() {
        totalTimeouts.increment();
    }

    public void recordLeak() {
        totalLeaksDetected.increment();
    }

    public void recordValidationFailure() {
        totalValidationFailures.increment();
    }

    public void updatePeakActive(long active) {
        long current;
        do {
            current = peakActiveConnections.get();
            if (active <= current)
                break;
        } while (!peakActiveConnections.compareAndSet(current, active));
    }

    public long getTotalAcquired() {
        return totalAcquired.sum();
    }

    public long getTotalReleased() {
        return totalReleased.sum();
    }

    public long getTotalCreated() {
        return totalCreated.sum();
    }

    public long getTotalDestroyed() {
        return totalDestroyed.sum();
    }

    public long getTotalTimeouts() {
        return totalTimeouts.sum();
    }

    public long getTotalLeaksDetected() {
        return totalLeaksDetected.sum();
    }

    public long getTotalValidationFailures() {
        return totalValidationFailures.sum();
    }

    public long getPeakActiveConnections() {
        return peakActiveConnections.get();
    }

    public double getAverageAcquireTimeMs() {
        long count = totalAcquired.sum();
        return count == 0 ? 0 : (totalAcquireTimeNanos.get() / (double) count) / 1_000_000.0;
    }

    public double getMaxAcquireTimeMs() {
        return maxAcquireTimeNanos.get() / 1_000_000.0;
    }

    @Override
    public String toString() {
        return String.format(
                "PoolMetrics[acquired=%d, released=%d, created=%d, destroyed=%d, " +
                        "timeouts=%d, leaks=%d, avgAcquireMs=%.2f, maxAcquireMs=%.2f, peakActive=%d]",
                getTotalAcquired(), getTotalReleased(), getTotalCreated(), getTotalDestroyed(),
                getTotalTimeouts(), getTotalLeaksDetected(),
                getAverageAcquireTimeMs(), getMaxAcquireTimeMs(), getPeakActiveConnections());
    }
}
