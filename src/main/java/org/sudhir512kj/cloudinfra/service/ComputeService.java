package org.sudhir512kj.cloudinfra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.cloudinfra.dto.CreateVMRequest;
import org.sudhir512kj.cloudinfra.model.*;
import org.sudhir512kj.cloudinfra.repository.ProvisioningTaskRepository;
import org.sudhir512kj.cloudinfra.repository.VirtualMachineRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComputeService {
    private final VirtualMachineRepository vmRepository;
    private final ProvisioningTaskRepository taskRepository;
    private final QuotaService quotaService;
    private final ProvisioningWorkerService workerService;
    
    @Transactional
    public VirtualMachine createVM(String accountId, CreateVMRequest request) {
        log.info("Creating VM: {} for account: {}", request.getName(), accountId);
        
        quotaService.checkAndIncrementQuota(request.getProjectId(), Quota.ResourceType.VM);
        
        VirtualMachine vm = new VirtualMachine();
        vm.setId("vm-" + UUID.randomUUID().toString().substring(0, 8));
        vm.setName(request.getName());
        vm.setRegion(request.getRegion());
        vm.setAccountId(accountId);
        vm.setState(ResourceState.PROVISIONING);
        vm.setInstanceType(request.getInstanceType());
        vm.setImageId(request.getImageId());
        vm.setVpcId(request.getVpcId());
        vm.setSubnetId(request.getSubnetId());
        vm.setSecurityGroupId(request.getSecurityGroupId());
        vm.setDiskGb(request.getDiskGb());
        
        setInstanceSpecs(vm, request.getInstanceType());
        
        vm = vmRepository.save(vm);
        
        ProvisioningTask task = new ProvisioningTask();
        task.setTaskId("task-" + UUID.randomUUID().toString().substring(0, 8));
        task.setResourceId(vm.getId());
        task.setTaskType(ProvisioningTask.TaskType.PROVISION);
        task.setStatus(ProvisioningTask.TaskStatus.PENDING);
        task.setRetryCount(0);
        task.setMaxRetries(3);
        task.setCreatedAt(LocalDateTime.now());
        taskRepository.save(task);
        
        workerService.processTask(task.getTaskId());
        
        return vm;
    }
    
    private void setInstanceSpecs(VirtualMachine vm, String instanceType) {
        switch (instanceType) {
            case "t2.micro" -> {
                vm.setVcpus(1);
                vm.setMemoryGb(1);
            }
            case "t2.small" -> {
                vm.setVcpus(1);
                vm.setMemoryGb(2);
            }
            case "t2.medium" -> {
                vm.setVcpus(2);
                vm.setMemoryGb(4);
            }
            case "t2.large" -> {
                vm.setVcpus(2);
                vm.setMemoryGb(8);
            }
            default -> {
                vm.setVcpus(1);
                vm.setMemoryGb(1);
            }
        }
    }
    
    @Transactional
    public void startVM(String vmId) {
        VirtualMachine vm = vmRepository.findById(vmId)
            .orElseThrow(() -> new RuntimeException("VM not found"));
        
        if (vm.getState() == ResourceState.STOPPED) {
            vm.setState(ResourceState.RUNNING);
            vmRepository.save(vm);
        }
    }
    
    @Transactional
    public void stopVM(String vmId) {
        VirtualMachine vm = vmRepository.findById(vmId)
            .orElseThrow(() -> new RuntimeException("VM not found"));
        
        if (vm.getState() == ResourceState.RUNNING) {
            vm.setState(ResourceState.STOPPED);
            vmRepository.save(vm);
        }
    }
    
    public List<VirtualMachine> listVMs(String accountId) {
        return vmRepository.findByAccountId(accountId);
    }
    
    public VirtualMachine getVM(String vmId) {
        return vmRepository.findById(vmId)
            .orElseThrow(() -> new RuntimeException("VM not found"));
    }
}
