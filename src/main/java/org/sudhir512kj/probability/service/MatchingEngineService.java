package org.sudhir512kj.probability.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sudhir512kj.probability.model.Order;
import org.sudhir512kj.probability.model.OrderSide;
import org.sudhir512kj.probability.model.OrderStatus;
import org.sudhir512kj.probability.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingEngineService {
    
    private final OrderRepository orderRepository;
    
    public void matchOrder(Order incomingOrder) {
        while (incomingOrder.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
            Order bestMatch = findBestMatch(incomingOrder);
            
            if (bestMatch == null || !pricesMatch(incomingOrder, bestMatch)) {
                break;
            }
            
            executeTrade(incomingOrder, bestMatch);
        }
        
        updateOrderStatus(incomingOrder);
    }
    
    private Order findBestMatch(Order order) {
        OrderSide oppositeSide = order.getSide() == OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY;
        
        List<Order> oppositeOrders = orderRepository.findByMarketIdAndOutcomeIdAndSideAndStatus(
            order.getMarketId(), order.getOutcomeId(), oppositeSide, OrderStatus.PENDING
        );
        
        return oppositeOrders.stream()
            .filter(o -> pricesMatch(order, o))
            .findFirst()
            .orElse(null);
    }
    
    private boolean pricesMatch(Order incoming, Order existing) {
        if (incoming.getSide() == OrderSide.BUY) {
            return incoming.getPrice().compareTo(existing.getPrice()) >= 0;
        } else {
            return incoming.getPrice().compareTo(existing.getPrice()) <= 0;
        }
    }
    
    private void executeTrade(Order incoming, Order existing) {
        BigDecimal tradeQty = incoming.getRemainingQuantity().min(existing.getRemainingQuantity());
        
        incoming.setFilledQuantity(incoming.getFilledQuantity().add(tradeQty));
        existing.setFilledQuantity(existing.getFilledQuantity().add(tradeQty));
        
        orderRepository.save(incoming);
        orderRepository.save(existing);
        
        log.info("Executed trade: {} shares at price {}", tradeQty, existing.getPrice());
    }
    
    private void updateOrderStatus(Order order) {
        if (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(OrderStatus.FILLED);
        } else if (order.getFilledQuantity().compareTo(BigDecimal.ZERO) > 0) {
            order.setStatus(OrderStatus.PARTIAL);
        }
        orderRepository.save(order);
    }
}
