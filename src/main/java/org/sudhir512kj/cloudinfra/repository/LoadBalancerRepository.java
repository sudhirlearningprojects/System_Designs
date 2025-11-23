package org.sudhir512kj.cloudinfra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.cloudinfra.model.LoadBalancer;

import java.util.List;

public interface LoadBalancerRepository extends JpaRepository<LoadBalancer, String> {
    List<LoadBalancer> findByAccountId(String accountId);
}
