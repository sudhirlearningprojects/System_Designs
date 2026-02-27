package org.sudhir512kj.netflix.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import org.sudhir512kj.netflix.model.UserProfile;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends CassandraRepository<UserProfile, UUID> {
}
