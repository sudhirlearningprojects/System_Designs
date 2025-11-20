package org.sudhir512kj.whatsapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.whatsapp.model.Chat;
import org.sudhir512kj.whatsapp.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, String> {
    
    @Query("SELECT c FROM Chat c JOIN c.participants p WHERE p = :user ORDER BY c.updatedAt DESC")
    List<Chat> findChatsByUser(User user);
    
    @Query("SELECT c FROM Chat c WHERE c.type = 'INDIVIDUAL' AND :user1 MEMBER OF c.participants AND :user2 MEMBER OF c.participants")
    Optional<Chat> findIndividualChat(User user1, User user2);
    
    @Query("SELECT c FROM Chat c WHERE c.type = 'GROUP' AND :user MEMBER OF c.participants")
    List<Chat> findGroupChatsByUser(User user);
}