package org.sudhir512kj.tiktok.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.tiktok.model.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
