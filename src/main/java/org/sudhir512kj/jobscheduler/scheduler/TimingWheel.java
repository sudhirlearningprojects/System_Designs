package org.sudhir512kj.jobscheduler.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Slf4j
public class TimingWheel {
    private final int wheelSize;
    private final long tickDuration; // in milliseconds
    private final TimingWheelBucket[] buckets;
    private volatile long currentTick;
    private final Map<String, ScheduledJob> jobIndex;
    private final ReentrantReadWriteLock lock;
    
    public TimingWheel() {
        this(3600, 1000); // 1 hour wheel with 1-second ticks
    }
    
    public TimingWheel(int wheelSize, long tickDuration) {
        this.wheelSize = wheelSize;
        this.tickDuration = tickDuration;
        this.buckets = new TimingWheelBucket[wheelSize];
        this.jobIndex = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.currentTick = System.currentTimeMillis() / tickDuration;
        
        // Initialize buckets
        for (int i = 0; i < wheelSize; i++) {
            buckets[i] = new TimingWheelBucket();
        }
        
        log.info("TimingWheel initialized with {} buckets, {} ms tick duration", 
                wheelSize, tickDuration);
    }
    
    public void addJob(ScheduledJob job, long delayMs) {
        lock.writeLock().lock();
        try {
            if (delayMs <= 0) {
                // Execute immediately
                job.setReadyForExecution(true);
                return;
            }
            
            long targetTick = currentTick + (delayMs / tickDuration);
            int bucketIndex = (int) (targetTick % wheelSize);
            
            job.setTargetTick(targetTick);
            buckets[bucketIndex].addJob(job);
            jobIndex.put(job.getJobId(), job);
            
            log.debug("Added job {} to bucket {} (target tick: {})", 
                    job.getJobId(), bucketIndex, targetTick);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public Set<ScheduledJob> tick() {
        lock.writeLock().lock();
        try {
            currentTick = System.currentTimeMillis() / tickDuration;
            int bucketIndex = (int) (currentTick % wheelSize);
            
            Set<ScheduledJob> readyJobs = new HashSet<>();
            TimingWheelBucket bucket = buckets[bucketIndex];
            
            Iterator<ScheduledJob> iterator = bucket.getJobs().iterator();
            while (iterator.hasNext()) {
                ScheduledJob job = iterator.next();
                if (job.getTargetTick() <= currentTick) {
                    readyJobs.add(job);
                    iterator.remove();
                    jobIndex.remove(job.getJobId());
                }
            }
            
            if (!readyJobs.isEmpty()) {
                log.debug("Tick {}: Found {} ready jobs in bucket {}", 
                        currentTick, readyJobs.size(), bucketIndex);
            }
            
            return readyJobs;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public boolean cancelJob(String jobId) {
        lock.writeLock().lock();
        try {
            ScheduledJob job = jobIndex.remove(jobId);
            if (job != null) {
                int bucketIndex = (int) (job.getTargetTick() % wheelSize);
                buckets[bucketIndex].removeJob(job);
                log.debug("Cancelled job {} from bucket {}", jobId, bucketIndex);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public int getJobCount() {
        lock.readLock().lock();
        try {
            return jobIndex.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public long getCurrentTick() {
        return currentTick;
    }
    
    private static class TimingWheelBucket {
        private final Set<ScheduledJob> jobs = ConcurrentHashMap.newKeySet();
        
        public void addJob(ScheduledJob job) {
            jobs.add(job);
        }
        
        public void removeJob(ScheduledJob job) {
            jobs.remove(job);
        }
        
        public Set<ScheduledJob> getJobs() {
            return jobs;
        }
    }
}