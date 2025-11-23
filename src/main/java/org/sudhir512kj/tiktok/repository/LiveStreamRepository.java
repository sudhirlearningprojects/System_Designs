package org.sudhir512kj.tiktok.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.tiktok.model.LiveStream;
import java.util.List;
import java.util.Optional;

public interface LiveStreamRepository extends JpaRepository<LiveStream, Long> {
    Optional<LiveStream> findByStreamKey(String streamKey);
    List<LiveStream> findByStatus(LiveStream.StreamStatus status);
    Optional<LiveStream> findByUserIdAndStatus(Long userId, LiveStream.StreamStatus status);
}
