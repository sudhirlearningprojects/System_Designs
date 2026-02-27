package org.sudhir512kj.alertmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.alertmanager.model.AlertRule;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, String> {
    List<AlertRule> findByProjectKeyAndEnabledTrue(String projectKey);
}
