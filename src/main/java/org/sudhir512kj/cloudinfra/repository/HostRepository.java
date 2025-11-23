package org.sudhir512kj.cloudinfra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.cloudinfra.model.Host;

import java.util.List;

public interface HostRepository extends JpaRepository<Host, String> {
    List<Host> findByRegionAndStatus(String region, Host.HostStatus status);
}
