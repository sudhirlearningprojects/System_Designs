package org.sudhir512kj.cloudinfra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.cloudinfra.model.Quota;

import java.util.Optional;

public interface QuotaRepository extends JpaRepository<Quota, Long> {
    Optional<Quota> findByProjectIdAndResourceType(String projectId, Quota.ResourceType resourceType);
}
