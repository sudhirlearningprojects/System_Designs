package org.sudhir512kj.cronjob.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "cron_locks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CronLock {
    @Id
    @Column(name = "lock_key")
    private String lockKey;

    @Column(name = "owner_node", nullable = false)
    private String ownerNode;

    @Column(name = "fencing_token", nullable = false)
    private Long fencingToken;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
