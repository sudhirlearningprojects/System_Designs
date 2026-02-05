package org.sudhir512kj.probability.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.probability.dto.CreateMarketRequest;
import org.sudhir512kj.probability.dto.MarketResponse;
import org.sudhir512kj.probability.model.*;
import org.sudhir512kj.probability.repository.MarketRepository;
import org.sudhir512kj.probability.repository.OutcomeRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketService {
    
    private final MarketRepository marketRepository;
    private final OutcomeRepository outcomeRepository;
    
    @Transactional
    public MarketResponse createMarket(CreateMarketRequest request, UUID creatorId) {
        Market market = Market.builder()
            .question(request.getQuestion())
            .description(request.getDescription())
            .marketType(request.getMarketType())
            .status(MarketStatus.OPEN)
            .endDate(request.getEndDate())
            .creatorId(creatorId)
            .liquidityParameter(request.getLiquidityParameter())
            .category(request.getCategory())
            .resolutionSource(request.getResolutionSource())
            .build();
        
        market = marketRepository.save(market);
        
        // Create outcomes
        for (String outcomeName : request.getOutcomes()) {
            Outcome outcome = Outcome.builder()
                .market(market)
                .name(outcomeName)
                .build();
            outcomeRepository.save(outcome);
        }
        
        log.info("Created market: {} with ID: {}", market.getQuestion(), market.getMarketId());
        return toResponse(market);
    }
    
    public MarketResponse getMarket(UUID marketId) {
        Market market = marketRepository.findById(marketId)
            .orElseThrow(() -> new RuntimeException("Market not found"));
        return toResponse(market);
    }
    
    public Page<MarketResponse> listMarkets(MarketStatus status, Pageable pageable) {
        Page<Market> markets = status != null 
            ? marketRepository.findByStatus(status, pageable)
            : marketRepository.findAll(pageable);
        return markets.map(this::toResponse);
    }
    
    private MarketResponse toResponse(Market market) {
        return MarketResponse.builder()
            .marketId(market.getMarketId())
            .question(market.getQuestion())
            .description(market.getDescription())
            .marketType(market.getMarketType())
            .status(market.getStatus())
            .endDate(market.getEndDate())
            .totalVolume(market.getTotalVolume())
            .createdAt(market.getCreatedAt())
            .build();
    }
}
