package org.sudhir512kj.cloudinfra.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "load_balancers")
public class LoadBalancer {
    @Id
    private String id;
    private String name;
    private String region;
    private String accountId;
    private String dnsName;
    private String vpcId;
    
    @Enumerated(EnumType.STRING)
    private LBType lbType;
    
    @Enumerated(EnumType.STRING)
    private LBScheme scheme;
    
    private Integer port;
    private String protocol;
    private String targetGroupId;
    
    public enum LBType {
        APPLICATION, NETWORK, GATEWAY
    }
    
    public enum LBScheme {
        INTERNET_FACING, INTERNAL
    }
}
