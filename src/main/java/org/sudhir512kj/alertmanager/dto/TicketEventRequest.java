package org.sudhir512kj.alertmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sudhir512kj.alertmanager.model.TicketEventType;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketEventRequest {
    private String ticketId;
    private String projectKey;
    private TicketEventType eventType;
    private String summary;
    private String description;
    private Map<String, String> metadata; // assignee, priority, status, oldValue, newValue, etc.
}
