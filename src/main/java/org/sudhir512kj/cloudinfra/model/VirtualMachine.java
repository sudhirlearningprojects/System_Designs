package org.sudhir512kj.cloudinfra.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "virtual_machines")
public class VirtualMachine {
    @Id
    private String id;
    private String name;
    private String region;
    private String accountId;
    
    @Enumerated(EnumType.STRING)
    private ResourceState state;
    
    private String instanceType;
    private Integer vcpus;
    private Integer memoryGb;
    private Integer diskGb;
    private String imageId;
    private String publicIp;
    private String privateIp;
    private String vpcId;
    private String subnetId;
    private String securityGroupId;
    private String hostId;
}
