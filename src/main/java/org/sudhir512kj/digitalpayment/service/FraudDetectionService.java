package org.sudhir512kj.digitalpayment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.digitalpayment.dto.PaymentInitiationRequest;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class FraudDetectionService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("100000");
    private static final int MAX_TRANSACTIONS_PER_HOUR = 50;
    
    public boolean isSuspicious(PaymentInitiationRequest request) {
        String userId = request.getSenderId();
        
        // Check daily transaction limit
        if (isDailyLimitExceeded(userId, request.getAmount())) {
            return true;
        }
        
        // Check transaction frequency
        if (isTransactionFrequencyHigh(userId)) {
            return true;
        }
        
        // Check amount threshold
        if (request.getAmount().compareTo(new BigDecimal("50000")) > 0) {
            return true; // Flag high-value transactions
        }
        
        return false;
    }
    
    private boolean isDailyLimitExceeded(String userId, BigDecimal amount) {
        String key = "daily_limit:" + userId;
        String currentAmountStr = (String) redisTemplate.opsForValue().get(key);
        
        BigDecimal currentAmount = currentAmountStr != null ? 
            new BigDecimal(currentAmountStr) : BigDecimal.ZERO;
        
        BigDecimal newAmount = currentAmount.add(amount);
        
        if (newAmount.compareTo(DAILY_LIMIT) > 0) {
            return true;
        }
        
        // Update daily amount
        redisTemplate.opsForValue().set(key, newAmount.toString(), 24, TimeUnit.HOURS);
        return false;
    }
    
    private boolean isTransactionFrequencyHigh(String userId) {
        String key = "txn_count:" + userId;
        String countStr = (String) redisTemplate.opsForValue().get(key);
        
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        
        if (count >= MAX_TRANSACTIONS_PER_HOUR) {
            return true;
        }
        
        // Increment counter
        redisTemplate.opsForValue().set(key, String.valueOf(count + 1), 1, TimeUnit.HOURS);
        return false;
    }
}