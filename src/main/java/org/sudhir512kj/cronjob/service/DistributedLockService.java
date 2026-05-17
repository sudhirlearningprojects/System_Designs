package org.sudhir512kj.cronjob.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.cronjob.model.CronLock;
import org.sudhir512kj.cronjob.repository.CronLockRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {
    private final CronLockRepository lockRepository;
    private final AtomicLong fencingTokenGenerator = new AtomicLong(System.currentTimeMillis());

    private static final int LOCK_TTL_SECONDS = 60;

    @Transactional
    public Optional<Long> tryAcquireLock(String lockKey, String nodeId) {
        lockRepository.deleteExpiredLocks(LocalDateTime.now());

        Optional<CronLock> existing = lockRepository.findById(lockKey);
        if (existing.isPresent() && existing.get().getExpiresAt().isAfter(LocalDateTime.now())) {
            return Optional.empty(); // Lock held by another node
        }

        long token = fencingTokenGenerator.incrementAndGet();
        CronLock lock = new CronLock();
        lock.setLockKey(lockKey);
        lock.setOwnerNode(nodeId);
        lock.setFencingToken(token);
        lock.setAcquiredAt(LocalDateTime.now());
        lock.setExpiresAt(LocalDateTime.now().plusSeconds(LOCK_TTL_SECONDS));
        lockRepository.save(lock);

        log.debug("Lock acquired: key={}, node={}, token={}", lockKey, nodeId, token);
        return Optional.of(token);
    }

    @Transactional
    public void releaseLock(String lockKey, String nodeId) {
        lockRepository.findById(lockKey).ifPresent(lock -> {
            if (lock.getOwnerNode().equals(nodeId)) {
                lockRepository.delete(lock);
            }
        });
    }
}
