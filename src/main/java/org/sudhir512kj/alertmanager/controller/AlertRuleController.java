package org.sudhir512kj.alertmanager.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.alertmanager.dto.AlertRuleRequest;
import org.sudhir512kj.alertmanager.model.AlertRule;
import org.sudhir512kj.alertmanager.service.AlertRuleService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class AlertRuleController {
    private final AlertRuleService alertRuleService;

    @PostMapping
    public ResponseEntity<AlertRule> createRule(@RequestBody AlertRuleRequest request) {
        return ResponseEntity.ok(alertRuleService.createRule(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertRule> updateRule(@PathVariable String id, @RequestBody AlertRuleRequest request) {
        return ResponseEntity.ok(alertRuleService.updateRule(id, request));
    }

    @GetMapping
    public ResponseEntity<List<AlertRule>> getAllRules() {
        return ResponseEntity.ok(alertRuleService.getAllRules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertRule> getRule(@PathVariable String id) {
        return ResponseEntity.ok(alertRuleService.getRule(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable String id) {
        alertRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
