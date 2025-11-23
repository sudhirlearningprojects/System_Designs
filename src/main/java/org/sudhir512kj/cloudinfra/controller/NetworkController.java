package org.sudhir512kj.cloudinfra.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.cloudinfra.dto.CreateVPCRequest;
import org.sudhir512kj.cloudinfra.model.VPC;
import org.sudhir512kj.cloudinfra.service.NetworkService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/network")
@RequiredArgsConstructor
public class NetworkController {
    private final NetworkService networkService;
    
    @PostMapping("/vpcs")
    public ResponseEntity<VPC> createVPC(
            @RequestHeader("X-Account-Id") String accountId,
            @RequestBody CreateVPCRequest request) {
        return ResponseEntity.ok(networkService.createVPC(accountId, request));
    }
    
    @GetMapping("/vpcs")
    public ResponseEntity<List<VPC>> listVPCs(
            @RequestHeader("X-Account-Id") String accountId) {
        return ResponseEntity.ok(networkService.listVPCs(accountId));
    }
}
