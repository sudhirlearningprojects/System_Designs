package org.sudhir512kj.alertmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sudhir512kj.alertmanager.model.Channel;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationChannelRequest {
    private String name;
    private Channel type;
    private Map<String, String> configuration;
    private Boolean enabled;
}
