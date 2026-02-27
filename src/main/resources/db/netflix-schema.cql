-- Netflix Cassandra Schema

CREATE KEYSPACE IF NOT EXISTS netflix
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 3};

USE netflix;

-- Content catalog table
CREATE TABLE IF NOT EXISTS content (
    content_id UUID PRIMARY KEY,
    title TEXT,
    type TEXT,
    genres LIST<TEXT>,
    release_year INT,
    description TEXT,
    duration_minutes INT,
    rating DOUBLE,
    view_count BIGINT,
    metadata MAP<TEXT, TEXT>,
    created_at TIMESTAMP
);

-- User profiles table
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id UUID PRIMARY KEY,
    email TEXT,
    subscription_tier TEXT,
    preferred_genres LIST<TEXT>,
    genre_affinity_scores MAP<TEXT, DOUBLE>,
    last_active TIMESTAMP,
    device_type TEXT,
    max_streams INT
);

-- Viewing activity table (time-series data)
CREATE TABLE IF NOT EXISTS viewing_activity (
    activity_id UUID PRIMARY KEY,
    user_id UUID,
    content_id UUID,
    watched_seconds INT,
    total_seconds INT,
    completion_rate DOUBLE,
    quality TEXT,
    device_id TEXT,
    timestamp TIMESTAMP,
    completed BOOLEAN
);

CREATE INDEX ON viewing_activity (user_id);
CREATE INDEX ON viewing_activity (content_id);

-- Viewing sessions table
CREATE TABLE IF NOT EXISTS viewing_sessions (
    session_id UUID PRIMARY KEY,
    user_id UUID,
    content_id UUID,
    device_id TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    watched_seconds INT,
    quality TEXT,
    bandwidth INT
);

CREATE INDEX ON viewing_sessions (user_id);

-- Encoded videos table
CREATE TABLE IF NOT EXISTS encoded_videos (
    encoding_id UUID PRIMARY KEY,
    content_id UUID,
    quality TEXT,
    codec TEXT,
    bitrate INT,
    cdn_url TEXT,
    file_size BIGINT,
    status TEXT
);

CREATE INDEX ON encoded_videos (content_id);
