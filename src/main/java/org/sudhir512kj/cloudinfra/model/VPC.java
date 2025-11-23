package org.sudhir512kj.cloudinfra.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "vpcs")
public class VPC {
    @Id
    private String id;
    private String name;
    private String region;
    private String accountId;
    private String cidrBlock;
    private Boolean enableDnsSupport;
    private Boolean enableDnsHostnames;
    private String internetGatewayId;
}
