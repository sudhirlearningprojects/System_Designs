package org.sudhir512kj.uber.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ElasticsearchService {
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchService.class);
    private final ElasticsearchClient client;
    
    @Value("${elasticsearch.index.rides:uber_rides}")
    private String ridesIndex;
    
    public ElasticsearchService(ElasticsearchClient client) {
        this.client = client;
    }
    
    public void indexRideForAnalytics(UUID rideId, String status, double fare, int durationMinutes) {
        try {
            Map<String, Object> rideDoc = new HashMap<>();
            rideDoc.put("ride_id", rideId.toString());
            rideDoc.put("status", status);
            rideDoc.put("fare", fare);
            rideDoc.put("duration_minutes", durationMinutes);
            rideDoc.put("timestamp", System.currentTimeMillis());
            
            client.index(IndexRequest.of(i -> i
                .index(ridesIndex)
                .id(rideId.toString())
                .document(rideDoc)
            ));
            
            log.debug("Indexed ride {} to Elasticsearch", rideId);
        } catch (Exception e) {
            log.error("Failed to index ride to Elasticsearch", e);
        }
    }
}
