package org.sudhir512kj.cloudflare.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.cloudflare.model.SecurityRule;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityRuleRepository extends JpaRepository<SecurityRule, UUID> {
    
    List<SecurityRule> findByZoneDomainAndEnabledTrue(String domain);
    
    List<SecurityRule> findByZoneIdAndEnabledTrue(UUID zoneId);
}