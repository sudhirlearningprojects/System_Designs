package org.sudhir512kj.cloudinfra.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.cloudinfra.dto.CreateVMRequest;
import org.sudhir512kj.cloudinfra.model.VirtualMachine;
import org.sudhir512kj.cloudinfra.service.ComputeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/compute")
@RequiredArgsConstructor
public class ComputeController {
    private final ComputeService computeService;
    
    @PostMapping("/vms")
    public ResponseEntity<VirtualMachine> createVM(
            @RequestHeader("X-Account-Id") String accountId,
            @RequestBody CreateVMRequest request) {
        return ResponseEntity.ok(computeService.createVM(accountId, request));
    }
    
    @GetMapping("/vms")
    public ResponseEntity<List<VirtualMachine>> listVMs(
            @RequestHeader("X-Account-Id") String accountId) {
        return ResponseEntity.ok(computeService.listVMs(accountId));
    }
    
    @GetMapping("/vms/{vmId}")
    public ResponseEntity<VirtualMachine> getVM(@PathVariable String vmId) {
        return ResponseEntity.ok(computeService.getVM(vmId));
    }
    
    @PostMapping("/vms/{vmId}/start")
    public ResponseEntity<Void> startVM(@PathVariable String vmId) {
        computeService.startVM(vmId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/vms/{vmId}/stop")
    public ResponseEntity<Void> stopVM(@PathVariable String vmId) {
        computeService.stopVM(vmId);
        return ResponseEntity.ok().build();
    }
}
