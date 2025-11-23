package org.sudhir512kj.cloudinfra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.cloudinfra.model.ManagedDatabase;

import java.util.List;

public interface ManagedDatabaseRepository extends JpaRepository<ManagedDatabase, String> {
    List<ManagedDatabase> findByAccountId(String accountId);
}
