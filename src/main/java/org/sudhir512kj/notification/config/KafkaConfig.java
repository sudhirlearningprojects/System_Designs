package org.sudhir512kj.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    
    @Bean
    public NewTopic criticalTopic() {
        return TopicBuilder.name("notifications.critical")
            .partitions(50)
            .replicas(3)
            .build();
    }
    
    @Bean
    public NewTopic highTopic() {
        return TopicBuilder.name("notifications.high")
            .partitions(100)
            .replicas(3)
            .build();
    }
    
    @Bean
    public NewTopic mediumTopic() {
        return TopicBuilder.name("notifications.medium")
            .partitions(200)
            .replicas(2)
            .build();
    }
    
    @Bean
    public NewTopic lowTopic() {
        return TopicBuilder.name("notifications.low")
            .partitions(200)
            .replicas(2)
            .build();
    }
}
