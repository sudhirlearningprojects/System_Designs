package org.sudhir512kj.tiktok.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.tiktok.model.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByStreamIdAndTimestampAfterOrderByTimestampDesc(
        Long streamId, LocalDateTime after);
    
    List<ChatMessage> findTop50ByStreamIdOrderByTimestampDesc(Long streamId);
}
