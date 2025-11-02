package org.sudhir512kj.digitalpayment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {
    
    private FraudDetection fraudDetection = new FraudDetection();
    private Idempotency idempotency = new Idempotency();
    
    public static class FraudDetection {
        private BigDecimal dailyLimit = new BigDecimal("100000");
        private int hourlyTransactionLimit = 50;
        
        public BigDecimal getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
        
        public int getHourlyTransactionLimit() { return hourlyTransactionLimit; }
        public void setHourlyTransactionLimit(int hourlyTransactionLimit) { this.hourlyTransactionLimit = hourlyTransactionLimit; }
    }
    
    public static class Idempotency {
        private int cacheDuration = 24; // hours
        
        public int getCacheDuration() { return cacheDuration; }
        public void setCacheDuration(int cacheDuration) { this.cacheDuration = cacheDuration; }
    }
    
    public FraudDetection getFraudDetection() { return fraudDetection; }
    public void setFraudDetection(FraudDetection fraudDetection) { this.fraudDetection = fraudDetection; }
    
    public Idempotency getIdempotency() { return idempotency; }
    public void setIdempotency(Idempotency idempotency) { this.idempotency = idempotency; }
}