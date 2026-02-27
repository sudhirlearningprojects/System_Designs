package org.sudhir512kj.alertmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.alertmanager.model.Alert;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {
}
