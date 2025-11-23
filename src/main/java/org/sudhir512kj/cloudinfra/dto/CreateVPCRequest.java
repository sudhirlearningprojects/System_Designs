package org.sudhir512kj.cloudinfra.dto;

import lombok.Data;

@Data
public class CreateVPCRequest {
    private String name;
    private String region;
    private String cidrBlock;
}
