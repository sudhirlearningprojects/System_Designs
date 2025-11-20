package org.sudhir512kj.whatsapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.whatsapp.model.Message;
import org.sudhir512kj.whatsapp.model.MessageDelivery;
import org.sudhir512kj.whatsapp.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageDeliveryRepository extends JpaRepository<MessageDelivery, String> {
    
    List<MessageDelivery> findByMessageAndUser(Message message, User user);
    
    @Modifying
    @Query("UPDATE MessageDelivery md SET md.status = 'READ', md.readAt = :readAt " +
           "WHERE md.message.chat.id = :chatId AND md.user = :user AND md.status != 'read'")
    void markMessagesAsRead(String chatId, User user, LocalDateTime readAt);
    
    @Query("SELECT md FROM MessageDelivery md WHERE md.message = :message")
    List<MessageDelivery> findByMessage(Message message);
}