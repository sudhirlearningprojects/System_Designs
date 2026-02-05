package org.sudhir512kj.probability.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.probability.dto.OrderRequest;
import org.sudhir512kj.probability.dto.OrderResponse;
import org.sudhir512kj.probability.service.TradingService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class TradingController {
    
    private final TradingService tradingService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@RequestBody OrderRequest request,
                                   @RequestHeader("X-User-Id") UUID userId) {
        return tradingService.placeOrder(request, userId);
    }
    
    @DeleteMapping("/{orderId}")
    public OrderResponse cancelOrder(@PathVariable UUID orderId,
                                    @RequestHeader("X-User-Id") UUID userId) {
        return tradingService.cancelOrder(orderId, userId);
    }
}
