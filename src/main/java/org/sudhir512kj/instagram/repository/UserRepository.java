package org.sudhir512kj.instagram.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.instagram.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.username LIKE %:query% OR u.fullName LIKE %:query%")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY u.followerCount DESC")
    java.util.List<User> searchByUsernameOrFullName(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.isVerified = true ORDER BY u.followerCount DESC")
    Page<User> findVerifiedUsers(Pageable pageable);
    
    java.util.List<User> findTopByOrderByFollowerCountDesc(Pageable pageable);
}