package org.sudhir512kj.cloudflare.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.sudhir512kj.cloudflare.model.SecurityRule;
import org.sudhir512kj.cloudflare.repository.SecurityRuleRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityRuleService {
    
    private final SecurityRuleRepository securityRuleRepository;
    
    public List<SecurityRule> getRulesForDomain(String domain) {
        return securityRuleRepository.findByZoneDomainAndEnabledTrue(domain);
    }
}