package org.sudhir512kj.cloudflare.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.sudhir512kj.cloudflare.model.DNSRecord;
import org.sudhir512kj.cloudflare.repository.DNSRecordRepository;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DNSService {
    
    private final DNSRecordRepository dnsRecordRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public DNSResponse resolveDomain(String domain, String recordType) {
        String cacheKey = "dns:" + domain + ":" + recordType;
        
        // Check cache first
        DNSResponse cached = (DNSResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Query database
        List<DNSRecord> records = dnsRecordRepository.findByNameAndType(domain, recordType);
        
        if (records.isEmpty()) {
            return DNSResponse.notFound();
        }
        
        DNSResponse response = DNSResponse.builder()
            .domain(domain)
            .type(recordType)
            .records(records)
            .ttl(records.get(0).getTtl())
            .build();
        
        // Cache response
        redisTemplate.opsForValue().set(cacheKey, response, 
                                      Duration.ofSeconds(response.getTtl()));
        
        return response;
    }
    
    public static class DNSResponse {
        private String domain;
        private String type;
        private List<DNSRecord> records;
        private Integer ttl;
        
        public static DNSResponse notFound() {
            return new DNSResponse();
        }
        
        public static DNSResponseBuilder builder() {
            return new DNSResponseBuilder();
        }
        
        public static class DNSResponseBuilder {
            private String domain;
            private String type;
            private List<DNSRecord> records;
            private Integer ttl;
            
            public DNSResponseBuilder domain(String domain) {
                this.domain = domain;
                return this;
            }
            
            public DNSResponseBuilder type(String type) {
                this.type = type;
                return this;
            }
            
            public DNSResponseBuilder records(List<DNSRecord> records) {
                this.records = records;
                return this;
            }
            
            public DNSResponseBuilder ttl(Integer ttl) {
                this.ttl = ttl;
                return this;
            }
            
            public DNSResponse build() {
                DNSResponse response = new DNSResponse();
                response.domain = this.domain;
                response.type = this.type;
                response.records = this.records;
                response.ttl = this.ttl;
                return response;
            }
        }
        
        public String getDomain() { return domain; }
        public String getType() { return type; }
        public List<DNSRecord> getRecords() { return records; }
        public Integer getTtl() { return ttl; }
    }
}