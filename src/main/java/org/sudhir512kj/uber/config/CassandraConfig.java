package org.sudhir512kj.uber.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.InetSocketAddress;

@Configuration
public class CassandraConfig {
    @Value("${spring.data.cassandra.contact-points:localhost}")
    private String contactPoints;

    @Value("${spring.data.cassandra.port:9042}")
    private int port;

    @Value("${spring.data.cassandra.keyspace-name:uber_keyspace}")
    private String keyspace;

    @Value("${spring.data.cassandra.local-datacenter:datacenter1}")
    private String datacenter;

    @Bean
    public CqlSession cqlSession() {
        return CqlSession.builder()
            .addContactPoint(new InetSocketAddress(contactPoints, port))
            .withLocalDatacenter(datacenter)
            .withKeyspace(keyspace)
            .build();
    }
}
