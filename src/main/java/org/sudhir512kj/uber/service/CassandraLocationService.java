package org.sudhir512kj.uber.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.model.Location;
import java.time.Instant;
import java.util.UUID;

@Service
public class CassandraLocationService {
    private static final Logger log = LoggerFactory.getLogger(CassandraLocationService.class);
    private final CqlSession session;
    private final PreparedStatement insertLocationStmt;
    
    public CassandraLocationService(CqlSession session) {
        this.session = session;
        this.insertLocationStmt = session.prepare(
            "INSERT INTO location_history (driver_id, timestamp, latitude, longitude) VALUES (?, ?, ?, ?)"
        );
    }
    
    public void saveLocationHistory(UUID driverId, Location location) {
        try {
            session.execute(insertLocationStmt.bind(
                driverId,
                Instant.now(),
                location.getLatitude(),
                location.getLongitude()
            ));
            log.debug("Saved location history for driver {}", driverId);
        } catch (Exception e) {
            log.error("Failed to save location to Cassandra", e);
        }
    }
}
