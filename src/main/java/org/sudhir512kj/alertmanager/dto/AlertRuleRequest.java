package org.sudhir512kj.alertmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sudhir512kj.alertmanager.model.TicketEventType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleRequest {
    private String name;
    private String description;
    private String projectKey;
    private List<TicketEventType> triggerEvents;
    private List<String> channelIds;
    private Boolean enabled;
    private String filterCondition;
}
