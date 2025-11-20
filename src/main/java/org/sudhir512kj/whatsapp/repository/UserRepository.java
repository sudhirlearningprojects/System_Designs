package org.sudhir512kj.whatsapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.whatsapp.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    @Query("SELECT u FROM User u WHERE u.phoneNumber LIKE %:query% OR u.name LIKE %:query%")
    List<User> searchUsers(String query);
    
    @Query("SELECT u FROM User u WHERE u.phoneNumber IN :phoneNumbers")
    List<User> findByPhoneNumbers(List<String> phoneNumbers);
}