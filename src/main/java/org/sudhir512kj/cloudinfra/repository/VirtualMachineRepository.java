package org.sudhir512kj.cloudinfra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.cloudinfra.model.VirtualMachine;

import java.util.List;

public interface VirtualMachineRepository extends JpaRepository<VirtualMachine, String> {
    List<VirtualMachine> findByAccountId(String accountId);
}
