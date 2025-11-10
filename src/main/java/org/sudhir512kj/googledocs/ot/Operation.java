package org.sudhir512kj.googledocs.ot;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operation {
    private OperationType type;
    private Integer position;
    private String text;
    private Integer length;
    private String userId;
    private Long timestamp;
    private Integer version;
    
    public enum OperationType {
        INSERT, DELETE, RETAIN
    }
}
