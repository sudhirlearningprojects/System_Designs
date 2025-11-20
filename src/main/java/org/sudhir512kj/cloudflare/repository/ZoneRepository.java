package org.sudhir512kj.cloudflare.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.cloudflare.model.Zone;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, UUID> {
    
    Optional<Zone> findByDomain(String domain);
    
    boolean existsByDomain(String domain);
}