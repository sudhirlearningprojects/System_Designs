package org.sudhir512kj.spotify.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.spotify.model.DownloadedTrack;
import java.util.List;
import java.util.Optional;

@Repository
public interface DownloadedTrackRepository extends JpaRepository<DownloadedTrack, String> {
    List<DownloadedTrack> findByUserIdAndDeviceId(String userId, String deviceId);
    Optional<DownloadedTrack> findByUserIdAndTrackIdAndDeviceId(String userId, String trackId, String deviceId);
    boolean existsByUserIdAndTrackIdAndDeviceId(String userId, String trackId, String deviceId);
}
