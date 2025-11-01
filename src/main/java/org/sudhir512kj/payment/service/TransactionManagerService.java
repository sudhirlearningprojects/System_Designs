package org.sudhir512kj.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.payment.model.PaymentTransaction;
import org.sudhir512kj.payment.model.RetryQueue;
import org.sudhir512kj.payment.repository.RetryQueueRepository;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionManagerService {
    private final RetryQueueRepository retryQueueRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Transactional
    public void scheduleRetry(PaymentTransaction transaction, Exception error) {
        log.info("Scheduling retry for transaction: {}", transaction.getId());
        
        RetryQueue retryEntry = new RetryQueue();
        retryEntry.setTransactionId(transaction.getId());
        retryEntry.setRetryCount(0);
        retryEntry.setMaxRetries(5);
        retryEntry.setNextRetryAt(calculateNextRetryTime(0));
        retryEntry.setErrorMessage(error.getMessage());
        
        retryQueueRepository.save(retryEntry);
        
        // Send to retry queue via Kafka
        kafkaTemplate.send("payment-retry-queue", transaction.getId().toString(), retryEntry);
        
        log.info("Retry scheduled for transaction: {} at {}", 
                transaction.getId(), retryEntry.getNextRetryAt());
    }
    
    @Async
    public CompletableFuture<Void> processRetryQueue() {
        log.debug("Processing retry queue");
        
        LocalDateTime now = LocalDateTime.now();
        var retryEntries = retryQueueRepository.findByNextRetryAtBeforeAndRetryCountLessThanMaxRetries(now);
        
        for (RetryQueue entry : retryEntries) {
            try {
                processRetryEntry(entry);
            } catch (Exception e) {
                log.error("Error processing retry entry: {}", entry.getId(), e);
            }
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    private void processRetryEntry(RetryQueue entry) {
        log.info("Processing retry entry for transaction: {}", entry.getTransactionId());
        
        entry.setRetryCount(entry.getRetryCount() + 1);
        
        if (entry.getRetryCount() >= entry.getMaxRetries()) {
            // Move to dead letter queue
            kafkaTemplate.send("payment-dead-letter-queue", 
                    entry.getTransactionId().toString(), entry);
            retryQueueRepository.delete(entry);
            log.warn("Transaction moved to dead letter queue: {}", entry.getTransactionId());
        } else {
            // Schedule next retry
            entry.setNextRetryAt(calculateNextRetryTime(entry.getRetryCount()));
            retryQueueRepository.save(entry);
            
            // Send retry message
            kafkaTemplate.send("payment-retry-queue", 
                    entry.getTransactionId().toString(), entry);
        }
    }
    
    private LocalDateTime calculateNextRetryTime(int retryCount) {
        // Exponential backoff with jitter: base_delay * (2^retry_count) + random_jitter
        long baseDelaySeconds = 30; // 30 seconds base delay
        long exponentialDelay = baseDelaySeconds * (1L << retryCount);
        long jitter = (long) (Math.random() * baseDelaySeconds);
        
        return LocalDateTime.now().plusSeconds(exponentialDelay + jitter);
    }
    
    @Transactional
    public void handleSagaCompensation(PaymentTransaction transaction) {
        log.info("Handling saga compensation for transaction: {}", transaction.getId());
        
        // Implement compensation logic
        // 1. Reverse any partial operations
        // 2. Update transaction status
        // 3. Notify relevant services
        
        kafkaTemplate.send("payment-compensation", 
                transaction.getId().toString(), transaction);
    }
}