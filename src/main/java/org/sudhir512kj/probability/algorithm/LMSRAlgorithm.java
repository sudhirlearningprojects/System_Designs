package org.sudhir512kj.probability.algorithm;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Logarithmic Market Scoring Rule (LMSR) implementation
 * Used for automated market making in prediction markets
 */
@Component
public class LMSRAlgorithm {
    
    /**
     * Calculate cost function: C(q) = b * ln(sum(exp(q_i / b)))
     */
    public BigDecimal calculateCost(Map<String, BigDecimal> shares, BigDecimal b) {
        double sum = shares.values().stream()
            .mapToDouble(q -> Math.exp(q.doubleValue() / b.doubleValue()))
            .sum();
        
        return b.multiply(BigDecimal.valueOf(Math.log(sum)))
            .setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate probability: p_i = exp(q_i/b) / sum(exp(q_j/b))
     */
    public BigDecimal calculateProbability(BigDecimal shares, Map<String, BigDecimal> allShares, BigDecimal b) {
        double numerator = Math.exp(shares.doubleValue() / b.doubleValue());
        double denominator = allShares.values().stream()
            .mapToDouble(q -> Math.exp(q.doubleValue() / b.doubleValue()))
            .sum();
        
        return BigDecimal.valueOf(numerator / denominator)
            .setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate price for buying shares
     */
    public BigDecimal calculateBuyPrice(String outcomeId, BigDecimal quantity, 
                                       Map<String, BigDecimal> currentShares, BigDecimal b) {
        BigDecimal costBefore = calculateCost(currentShares, b);
        
        Map<String, BigDecimal> newShares = Map.copyOf(currentShares);
        newShares.put(outcomeId, currentShares.get(outcomeId).add(quantity));
        
        BigDecimal costAfter = calculateCost(newShares, b);
        
        return costAfter.subtract(costBefore).setScale(4, RoundingMode.HALF_UP);
    }
}
