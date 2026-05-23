package org.sudhir512kj.connectionpool.config;

import org.sudhir512kj.connectionpool.health.ConnectionPoolHealthIndicator;
import org.sudhir512kj.connectionpool.model.PoolConfig;
import org.sudhir512kj.connectionpool.pool.ConnectionFactory;
import org.sudhir512kj.connectionpool.pool.JdbcConnectionFactory;
import org.sudhir512kj.connectionpool.service.ConnectionPoolService;

import java.time.Duration;

public class ConnectionPoolConfiguration {

    public static ConnectionPoolService createDefault(String jdbcUrl, String username, String password) {
        PoolConfig config = PoolConfig.builder()
                .poolName("DefaultPool")
                .minIdle(5)
                .maxIdle(10)
                .maxTotal(20)
                .maxWait(Duration.ofSeconds(30))
                .connectionTimeout(Duration.ofSeconds(5))
                .maxLifetime(Duration.ofMinutes(30))
                .minEvictableIdleTime(Duration.ofMinutes(10))
                .timeBetweenEvictionRuns(Duration.ofMinutes(5))
                .validationInterval(Duration.ofSeconds(30))
                .validationQuery("SELECT 1")
                .testOnBorrow(true)
                .testOnReturn(false)
                .testWhileIdle(true)
                .leakDetectionThreshold(Duration.ofSeconds(60))
                .keepaliveTime(Duration.ofMinutes(2))
                .build();

        ConnectionFactory factory = new JdbcConnectionFactory(jdbcUrl, username, password);
        return new ConnectionPoolService(config, factory);
    }

    public static ConnectionPoolService createHighThroughput(String jdbcUrl, String username, String password) {
        PoolConfig config = PoolConfig.builder()
                .poolName("HighThroughputPool")
                .minIdle(10)
                .maxIdle(30)
                .maxTotal(50)
                .maxWait(Duration.ofSeconds(5))
                .connectionTimeout(Duration.ofSeconds(3))
                .maxLifetime(Duration.ofMinutes(15))
                .minEvictableIdleTime(Duration.ofMinutes(5))
                .timeBetweenEvictionRuns(Duration.ofMinutes(2))
                .validationInterval(Duration.ofSeconds(15))
                .testOnBorrow(false) // Skip for speed; rely on testWhileIdle
                .testWhileIdle(true)
                .leakDetectionThreshold(Duration.ofSeconds(30))
                .build();

        ConnectionFactory factory = new JdbcConnectionFactory(jdbcUrl, username, password);
        return new ConnectionPoolService(config, factory);
    }

    public static ConnectionPoolService createMinimal(String jdbcUrl, String username, String password) {
        PoolConfig config = PoolConfig.builder()
                .poolName("MinimalPool")
                .minIdle(1)
                .maxIdle(3)
                .maxTotal(5)
                .maxWait(Duration.ofSeconds(10))
                .maxLifetime(Duration.ofMinutes(30))
                .minEvictableIdleTime(Duration.ofMinutes(2))
                .testOnBorrow(true)
                .testWhileIdle(true)
                .leakDetectionThreshold(Duration.ofSeconds(120))
                .build();

        ConnectionFactory factory = new JdbcConnectionFactory(jdbcUrl, username, password);
        return new ConnectionPoolService(config, factory);
    }

    public static ConnectionPoolHealthIndicator healthIndicator(ConnectionPoolService poolService) {
        return new ConnectionPoolHealthIndicator(poolService);
    }
}
