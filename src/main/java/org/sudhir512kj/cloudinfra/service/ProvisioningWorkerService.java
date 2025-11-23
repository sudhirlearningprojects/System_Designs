package org.sudhir512kj.cloudinfra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.cloudinfra.model.*;
import org.sudhir512kj.cloudinfra.repository.ProvisioningTaskRepository;
import org.sudhir512kj.cloudinfra.repository.VirtualMachineRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProvisioningWorkerService {
    private final ProvisioningTaskRepository taskRepository;
    private final VirtualMachineRepository vmRepository;
    private final PlacementService placementService;
    private final HypervisorAgentService hypervisorService;
    
    @Async
    @Transactional
    public void processTask(String taskId) {
        ProvisioningTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
        
        if (task.getStatus() != ProvisioningTask.TaskStatus.PENDING) {
            return;
        }
        
        task.setStatus(ProvisioningTask.TaskStatus.PROCESSING);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        
        try {
            VirtualMachine vm = vmRepository.findById(task.getResourceId())
                .orElseThrow(() -> new RuntimeException("VM not found"));
            
            // Select host using placement algorithm
            Host host = placementService.selectHost(vm.getVcpus(), vm.getMemoryGb(), vm.getRegion());
            
            // Allocate resources on host
            placementService.allocateResources(host.getHostId(), vm.getVcpus(), vm.getMemoryGb());
            
            // Call hypervisor agent to boot VM
            hypervisorService.bootVM(host.getHostId(), vm);
            
            // Update VM state
            vm.setState(ResourceState.RUNNING);
            vmRepository.save(vm);
            
            // Mark task completed
            task.setStatus(ProvisioningTask.TaskStatus.COMPLETED);
            task.setUpdatedAt(LocalDateTime.now());
            taskRepository.save(task);
            
            log.info("Successfully provisioned VM: {}", vm.getId());
            
        } catch (Exception e) {
            log.error("Failed to provision task: {}", taskId, e);
            handleTaskFailure(task, e.getMessage());
        }
    }
    
    private void handleTaskFailure(ProvisioningTask task, String errorMessage) {
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(errorMessage);
        task.setUpdatedAt(LocalDateTime.now());
        
        if (task.getRetryCount() >= task.getMaxRetries()) {
            task.setStatus(ProvisioningTask.TaskStatus.DEAD_LETTER);
            
            // Mark resource as error
            vmRepository.findById(task.getResourceId()).ifPresent(vm -> {
                vm.setState(ResourceState.ERROR);
                vmRepository.save(vm);
            });
        } else {
            task.setStatus(ProvisioningTask.TaskStatus.PENDING);
        }
        
        taskRepository.save(task);
    }
}
