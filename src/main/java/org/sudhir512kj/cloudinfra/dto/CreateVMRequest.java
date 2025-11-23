package org.sudhir512kj.cloudinfra.dto;

import lombok.Data;

@Data
public class CreateVMRequest {
    private String name;
    private String region;
    private String instanceType;
    private String imageId;
    private String vpcId;
    private String subnetId;
    private String securityGroupId;
    private Integer diskGb;
    private String projectId;
}
