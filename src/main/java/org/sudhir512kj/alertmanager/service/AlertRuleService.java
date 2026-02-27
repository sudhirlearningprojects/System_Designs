package org.sudhir512kj.alertmanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.alertmanager.dto.AlertRuleRequest;
import org.sudhir512kj.alertmanager.model.AlertRule;
import org.sudhir512kj.alertmanager.repository.AlertRuleRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertRuleService {
    private final AlertRuleRepository alertRuleRepository;

    @Transactional
    public AlertRule createRule(AlertRuleRequest request) {
        AlertRule rule = AlertRule.builder()
            .name(request.getName())
            .description(request.getDescription())
            .projectKey(request.getProjectKey())
            .triggerEvents(request.getTriggerEvents())
            .channelIds(request.getChannelIds())
            .enabled(request.getEnabled() != null ? request.getEnabled() : true)
            .filterCondition(request.getFilterCondition())
            .build();
        
        return alertRuleRepository.save(rule);
    }

    @Transactional
    public AlertRule updateRule(String id, AlertRuleRequest request) {
        AlertRule rule = alertRuleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rule not found"));
        
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setTriggerEvents(request.getTriggerEvents());
        rule.setChannelIds(request.getChannelIds());
        rule.setEnabled(request.getEnabled());
        rule.setFilterCondition(request.getFilterCondition());
        
        return alertRuleRepository.save(rule);
    }

    public List<AlertRule> getAllRules() {
        return alertRuleRepository.findAll();
    }

    public AlertRule getRule(String id) {
        return alertRuleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rule not found"));
    }

    @Transactional
    public void deleteRule(String id) {
        alertRuleRepository.deleteById(id);
    }
}
