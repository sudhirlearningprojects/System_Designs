package org.sudhir512kj.jobscheduler.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.jobscheduler.dto.JobRequest;
import org.sudhir512kj.jobscheduler.dto.JobResponse;
import org.sudhir512kj.jobscheduler.model.JobExecution;
import org.sudhir512kj.jobscheduler.service.JobManagementService;
import org.sudhir512kj.jobscheduler.repository.JobExecutionRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobManagementService jobManagementService;
    private final JobExecutionRepository executionRepository;
    
    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody JobRequest request) {
        JobResponse response = jobManagementService.submitJob(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID jobId) {
        JobResponse response = jobManagementService.getJobStatus(jobId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(@PathVariable UUID jobId) {
        jobManagementService.cancelJob(jobId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{jobId}/pause")
    public ResponseEntity<Void> pauseJob(@PathVariable UUID jobId) {
        jobManagementService.pauseJob(jobId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{jobId}/resume")
    public ResponseEntity<Void> resumeJob(@PathVariable UUID jobId) {
        jobManagementService.resumeJob(jobId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{jobId}/executions")
    public ResponseEntity<List<JobExecution>> getJobExecutions(@PathVariable UUID jobId) {
        List<JobExecution> executions = executionRepository.findByJobIdOrderByStartedAtDesc(jobId);
        return ResponseEntity.ok(executions);
    }
}