package org.sudhir512kj.netflix.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.netflix.model.EncodedVideo;
import org.sudhir512kj.netflix.model.VideoQuality;
import java.util.List;
import java.util.UUID;

@Repository
public interface EncodedVideoRepository extends CassandraRepository<EncodedVideo, UUID> {
    List<EncodedVideo> findByContentId(UUID contentId);
    List<EncodedVideo> findByContentIdAndQuality(UUID contentId, VideoQuality quality);
}
