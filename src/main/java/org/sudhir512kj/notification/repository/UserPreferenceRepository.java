package org.sudhir512kj.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sudhir512kj.notification.model.UserPreference;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, String> {
}
