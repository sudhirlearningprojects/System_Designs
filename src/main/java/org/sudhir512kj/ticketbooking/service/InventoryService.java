package org.sudhir512kj.ticketbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.ticketbooking.model.TicketType;
import org.sudhir512kj.ticketbooking.repository.TicketTypeRepository;

import java.util.concurrent.TimeUnit;

@Service
public class InventoryService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private TicketTypeRepository ticketTypeRepository;
    
    private static final String INVENTORY_KEY = "inventory:";
    private static final String HOLD_KEY = "hold:";
    private static final int HOLD_DURATION_MINUTES = 10;
    
    @Transactional
    public boolean holdTickets(Long ticketTypeId, Integer quantity, String holdId) {
        String inventoryKey = INVENTORY_KEY + ticketTypeId;
        String holdKey = HOLD_KEY + holdId;
        
        // Check Redis inventory first
        Integer available = (Integer) redisTemplate.opsForValue().get(inventoryKey);
        if (available == null) {
            // Initialize from database
            TicketType ticketType = ticketTypeRepository.findById(ticketTypeId).orElse(null);
            if (ticketType == null) return false;
            available = ticketType.getAvailableQuantity();
            redisTemplate.opsForValue().set(inventoryKey, available);
        }
        
        if (available < quantity) {
            return false;
        }
        
        // Atomic decrement in Redis
        Long newAvailable = redisTemplate.opsForValue().decrement(inventoryKey, quantity);
        if (newAvailable < 0) {
            // Rollback
            redisTemplate.opsForValue().increment(inventoryKey, quantity);
            return false;
        }
        
        // Set hold with TTL
        redisTemplate.opsForValue().set(holdKey, quantity, HOLD_DURATION_MINUTES, TimeUnit.MINUTES);
        
        return true;
    }
    
    @Transactional
    public void confirmHold(String holdId, Long ticketTypeId) {
        String holdKey = HOLD_KEY + holdId;
        Integer quantity = (Integer) redisTemplate.opsForValue().get(holdKey);
        
        if (quantity != null) {
            // Update database
            ticketTypeRepository.decrementAvailableQuantity(ticketTypeId, quantity);
            // Remove hold
            redisTemplate.delete(holdKey);
        }
    }
    
    @Transactional
    public void releaseHold(String holdId, Long ticketTypeId) {
        String holdKey = HOLD_KEY + holdId;
        String inventoryKey = INVENTORY_KEY + ticketTypeId;
        
        Integer quantity = (Integer) redisTemplate.opsForValue().get(holdKey);
        if (quantity != null) {
            // Return to Redis inventory
            redisTemplate.opsForValue().increment(inventoryKey, quantity);
            // Remove hold
            redisTemplate.delete(holdKey);
        }
    }
}