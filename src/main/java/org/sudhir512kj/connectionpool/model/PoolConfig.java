package org.sudhir512kj.connectionpool.model;

import java.time.Duration;

public class PoolConfig {

    private int minIdle = 5;
    private int maxIdle = 10;
    private int maxTotal = 20;
    private Duration maxWait = Duration.ofSeconds(30);
    private Duration minEvictableIdleTime = Duration.ofMinutes(10);
    private Duration timeBetweenEvictionRuns = Duration.ofMinutes(5);
    private Duration validationInterval = Duration.ofSeconds(30);
    private String validationQuery = "SELECT 1";
    private boolean testOnBorrow = true;
    private boolean testOnReturn = false;
    private boolean testWhileIdle = true;
    private Duration connectionTimeout = Duration.ofSeconds(5);
    private Duration leakDetectionThreshold = Duration.ofSeconds(60);
    private Duration maxLifetime = Duration.ofMinutes(30);
    private Duration keepaliveTime = Duration.ofMinutes(2);
    private String poolName = "DefaultPool";

    private PoolConfig() {}

    public static Builder builder() {
        return new Builder();
    }

    public int getMinIdle() { return minIdle; }
    public int getMaxIdle() { return maxIdle; }
    public int getMaxTotal() { return maxTotal; }
    public Duration getMaxWait() { return maxWait; }
    public Duration getMinEvictableIdleTime() { return minEvictableIdleTime; }
    public Duration getTimeBetweenEvictionRuns() { return timeBetweenEvictionRuns; }
    public Duration getValidationInterval() { return validationInterval; }
    public String getValidationQuery() { return validationQuery; }
    public boolean isTestOnBorrow() { return testOnBorrow; }
    public boolean isTestOnReturn() { return testOnReturn; }
    public boolean isTestWhileIdle() { return testWhileIdle; }
    public Duration getConnectionTimeout() { return connectionTimeout; }
    public Duration getLeakDetectionThreshold() { return leakDetectionThreshold; }
    public Duration getMaxLifetime() { return maxLifetime; }
    public Duration getKeepaliveTime() { return keepaliveTime; }
    public String getPoolName() { return poolName; }

    public static class Builder {
        private final PoolConfig config = new PoolConfig();

        public Builder minIdle(int minIdle) { config.minIdle = minIdle; return this; }
        public Builder maxIdle(int maxIdle) { config.maxIdle = maxIdle; return this; }
        public Builder maxTotal(int maxTotal) { config.maxTotal = maxTotal; return this; }
        public Builder maxWait(Duration maxWait) { config.maxWait = maxWait; return this; }
        public Builder minEvictableIdleTime(Duration d) { config.minEvictableIdleTime = d; return this; }
        public Builder timeBetweenEvictionRuns(Duration d) { config.timeBetweenEvictionRuns = d; return this; }
        public Builder validationInterval(Duration d) { config.validationInterval = d; return this; }
        public Builder validationQuery(String q) { config.validationQuery = q; return this; }
        public Builder testOnBorrow(boolean b) { config.testOnBorrow = b; return this; }
        public Builder testOnReturn(boolean b) { config.testOnReturn = b; return this; }
        public Builder testWhileIdle(boolean b) { config.testWhileIdle = b; return this; }
        public Builder connectionTimeout(Duration d) { config.connectionTimeout = d; return this; }
        public Builder leakDetectionThreshold(Duration d) { config.leakDetectionThreshold = d; return this; }
        public Builder maxLifetime(Duration d) { config.maxLifetime = d; return this; }
        public Builder keepaliveTime(Duration d) { config.keepaliveTime = d; return this; }
        public Builder poolName(String name) { config.poolName = name; return this; }

        public PoolConfig build() {
            validate();
            return config;
        }

        private void validate() {
            if (config.maxTotal < 1) throw new IllegalArgumentException("maxTotal must be >= 1");
            if (config.minIdle < 0) throw new IllegalArgumentException("minIdle must be >= 0");
            if (config.minIdle > config.maxTotal) throw new IllegalArgumentException("minIdle cannot exceed maxTotal");
            if (config.maxIdle > config.maxTotal) throw new IllegalArgumentException("maxIdle cannot exceed maxTotal");
            if (config.maxIdle < config.minIdle) config.maxIdle = config.minIdle;
        }
    }
}
