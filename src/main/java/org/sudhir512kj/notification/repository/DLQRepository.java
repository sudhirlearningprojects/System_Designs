package org.sudhir512kj.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.sudhir512kj.notification.model.DLQEntry;
import java.util.List;

public interface DLQRepository extends JpaRepository<DLQEntry, String> {
    @Query("SELECT d FROM DLQEntry d WHERE d.reprocessed = false ORDER BY d.createdAt ASC")
    List<DLQEntry> findReprocessable();
}
