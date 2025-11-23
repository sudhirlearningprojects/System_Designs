package org.sudhir512kj.cloudinfra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.cloudinfra.model.VPC;

import java.util.List;

public interface VPCRepository extends JpaRepository<VPC, String> {
    List<VPC> findByAccountId(String accountId);
}
