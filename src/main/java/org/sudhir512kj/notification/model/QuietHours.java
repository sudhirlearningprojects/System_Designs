package org.sudhir512kj.notification.model;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.time.LocalTime;

@Embeddable
@Data
public class QuietHours {
    private LocalTime start;
    private LocalTime end;
}
