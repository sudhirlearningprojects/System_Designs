package org.sudhir512kj.probability.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.probability.dto.CreateMarketRequest;
import org.sudhir512kj.probability.dto.MarketResponse;
import org.sudhir512kj.probability.model.MarketStatus;
import org.sudhir512kj.probability.service.MarketService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/markets")
@RequiredArgsConstructor
public class MarketController {
    
    private final MarketService marketService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MarketResponse createMarket(@RequestBody CreateMarketRequest request,
                                      @RequestHeader("X-User-Id") UUID userId) {
        return marketService.createMarket(request, userId);
    }
    
    @GetMapping("/{marketId}")
    public MarketResponse getMarket(@PathVariable UUID marketId) {
        return marketService.getMarket(marketId);
    }
    
    @GetMapping
    public Page<MarketResponse> listMarkets(@RequestParam(required = false) MarketStatus status,
                                           Pageable pageable) {
        return marketService.listMarkets(status, pageable);
    }
}
