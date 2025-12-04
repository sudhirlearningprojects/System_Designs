package org.sudhir512kj.spotify.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cassandra.config.AbstractReactiveCassandraConfiguration;
import org.springframework.data.cassandra.repository.config.EnableReactiveCassandraRepositories;

@Configuration
@Profile("spotify")
public class SpotifyConfig {
    
    @Bean
    public ElasticsearchClient elasticsearchClient(
            @Value("${elasticsearch.host:localhost}") String host,
            @Value("${elasticsearch.port:9200}") int port) {
        RestClient restClient = RestClient.builder(new HttpHost(host, port)).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}

@Configuration
@Profile("spotify")
@EnableReactiveCassandraRepositories(basePackages = "org.sudhir512kj.spotify.repository")
class CassandraConfig extends AbstractReactiveCassandraConfiguration {
    
    @Value("${cassandra.keyspace:spotify}")
    private String keyspace;
    
    @Value("${cassandra.contact-points:localhost}")
    private String contactPoints;
    
    @Value("${cassandra.port:9042}")
    private int port;
    
    @Override
    protected String getKeyspaceName() {
        return keyspace;
    }
    
    @Override
    protected String getContactPoints() {
        return contactPoints;
    }
    
    @Override
    protected int getPort() {
        return port;
    }
}
