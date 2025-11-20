package org.sudhir512kj.whatsapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.whatsapp.model.Chat;
import org.sudhir512kj.whatsapp.model.Message;
import org.sudhir512kj.whatsapp.model.User;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    
    Page<Message> findByChatOrderByCreatedAtDesc(Chat chat, Pageable pageable);
    
    @Query("SELECT m FROM Message m WHERE m.chat = :chat AND m.content LIKE %:query%")
    List<Message> searchMessagesInChat(Chat chat, String query);
    
    @Query("SELECT COUNT(m) FROM Message m JOIN MessageDelivery md ON m.id = md.message.id " +
           "WHERE m.chat = :chat AND md.user = :user AND md.status != 'READ'")
    Integer countUnreadMessages(Chat chat, User user);
    
    @Query("SELECT m FROM Message m WHERE m.chat = :chat ORDER BY m.createdAt DESC LIMIT 1")
    Message findLastMessageInChat(Chat chat);
}