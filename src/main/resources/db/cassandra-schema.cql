-- Cassandra Schema for Location History

CREATE KEYSPACE IF NOT EXISTS uber_keyspace 
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE uber_keyspace;

CREATE TABLE IF NOT EXISTS location_history (
    driver_id UUID,
    timestamp TIMESTAMP,
    latitude DOUBLE,
    longitude DOUBLE,
    PRIMARY KEY (driver_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);
