package org.sudhir512kj.cloudinfra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.cloudinfra.model.StorageBucket;

import java.util.List;
import java.util.Optional;

public interface StorageBucketRepository extends JpaRepository<StorageBucket, String> {
    List<StorageBucket> findByAccountId(String accountId);
    Optional<StorageBucket> findByBucketName(String bucketName);
    boolean existsByBucketName(String bucketName);
}
