package org.sudhir512kj.netflix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.netflix.model.EncodedVideo;
import org.sudhir512kj.netflix.model.VideoQuality;
import java.util.List;
import java.util.UUID;

@Repository
public interface EncodedVideoRepository extends JpaRepository<EncodedVideo, UUID> {
    List<EncodedVideo> findByContentId(UUID contentId);
    List<EncodedVideo> findByContentIdAndQuality(UUID contentId, VideoQuality quality);
}
