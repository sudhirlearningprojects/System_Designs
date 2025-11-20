package org.sudhir512kj.cloudflare.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.cloudflare.model.DNSRecord;

import java.util.List;
import java.util.UUID;

@Repository
public interface DNSRecordRepository extends JpaRepository<DNSRecord, UUID> {
    
    List<DNSRecord> findByNameAndType(String name, String type);
    
    List<DNSRecord> findByZoneId(UUID zoneId);
}