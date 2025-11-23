package org.sudhir512kj.cloudinfra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.cloudinfra.model.ProvisioningTask;

import java.util.List;

public interface ProvisioningTaskRepository extends JpaRepository<ProvisioningTask, String> {
    List<ProvisioningTask> findByStatus(ProvisioningTask.TaskStatus status);
}
