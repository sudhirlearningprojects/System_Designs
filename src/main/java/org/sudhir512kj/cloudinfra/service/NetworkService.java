package org.sudhir512kj.cloudinfra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.cloudinfra.dto.CreateVPCRequest;
import org.sudhir512kj.cloudinfra.model.VPC;
import org.sudhir512kj.cloudinfra.repository.VPCRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkService {
    private final VPCRepository vpcRepository;
    
    @Transactional
    public VPC createVPC(String accountId, CreateVPCRequest request) {
        VPC vpc = new VPC();
        vpc.setId("vpc-" + UUID.randomUUID().toString().substring(0, 8));
        vpc.setName(request.getName());
        vpc.setRegion(request.getRegion());
        vpc.setAccountId(accountId);
        vpc.setCidrBlock(request.getCidrBlock());
        vpc.setEnableDnsSupport(true);
        vpc.setEnableDnsHostnames(true);
        
        return vpcRepository.save(vpc);
    }
    
    public List<VPC> listVPCs(String accountId) {
        return vpcRepository.findByAccountId(accountId);
    }
}
