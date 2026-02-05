package org.sudhir512kj.probability.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.probability.dto.OrderRequest;
import org.sudhir512kj.probability.dto.OrderResponse;
import org.sudhir512kj.probability.model.*;
import org.sudhir512kj.probability.repository.OrderRepository;
import org.sudhir512kj.probability.repository.PositionRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingService {
    
    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final MatchingEngineService matchingEngine;
    
    @Transactional
    public OrderResponse placeOrder(OrderRequest request, UUID userId) {
        // Validate and create order
        Order order = Order.builder()
            .marketId(request.getMarketId())
            .outcomeId(request.getOutcomeId())
            .userId(userId)
            .orderType(request.getOrderType())
            .side(request.getSide())
            .price(request.getPrice())
            .quantity(request.getQuantity())
            .status(OrderStatus.PENDING)
            .idempotencyKey(request.getIdempotencyKey())
            .build();
        
        order = orderRepository.save(order);
        
        // Match order
        matchingEngine.matchOrder(order);
        
        log.info("Placed order: {} for user: {}", order.getOrderId(), userId);
        return toResponse(order);
    }
    
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        log.info("Cancelled order: {}", orderId);
        return toResponse(order);
    }
    
    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
            .orderId(order.getOrderId())
            .status(order.getStatus())
            .filledQuantity(order.getFilledQuantity())
            .remainingQuantity(order.getRemainingQuantity())
            .createdAt(order.getCreatedAt())
            .build();
    }
}
