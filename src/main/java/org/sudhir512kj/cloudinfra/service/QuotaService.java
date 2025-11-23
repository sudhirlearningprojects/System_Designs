package org.sudhir512kj.cloudinfra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.cloudinfra.model.Quota;
import org.sudhir512kj.cloudinfra.repository.QuotaRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {
    private final QuotaRepository quotaRepository;
    
    @Transactional
    public void checkAndIncrementQuota(String projectId, Quota.ResourceType resourceType) {
        Quota quota = quotaRepository.findByProjectIdAndResourceType(projectId, resourceType)
            .orElseThrow(() -> new RuntimeException("Quota not found"));
        
        if (quota.getCurrentCount() >= quota.getMaxCount()) {
            throw new RuntimeException("Quota exceeded for " + resourceType);
        }
        
        quota.setCurrentCount(quota.getCurrentCount() + 1);
        quotaRepository.save(quota);
    }
    
    @Transactional
    public void decrementQuota(String projectId, Quota.ResourceType resourceType) {
        Quota quota = quotaRepository.findByProjectIdAndResourceType(projectId, resourceType)
            .orElseThrow(() -> new RuntimeException("Quota not found"));
        
        quota.setCurrentCount(Math.max(0, quota.getCurrentCount() - 1));
        quotaRepository.save(quota);
    }
}
