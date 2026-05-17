package org.sudhir512kj.cronjob.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.cronjob.dto.CronJobRequest;
import org.sudhir512kj.cronjob.dto.CronJobResponse;
import org.sudhir512kj.cronjob.dto.DAGRequest;
import org.sudhir512kj.cronjob.model.CronDAG;
import org.sudhir512kj.cronjob.model.CronJobExecution;
import org.sudhir512kj.cronjob.service.CronJobService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cron")
@RequiredArgsConstructor
public class CronJobController {
    private final CronJobService cronJobService;

    @PostMapping("/jobs")
    public ResponseEntity<CronJobResponse> createJob(@Valid @RequestBody CronJobRequest request) {
        return ResponseEntity.ok(cronJobService.createJob(request));
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<CronJobResponse> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(cronJobService.getJob(id));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<CronJobResponse>> listJobs(@RequestParam String namespace) {
        return ResponseEntity.ok(cronJobService.listJobs(namespace));
    }

    @PostMapping("/jobs/{id}/pause")
    public ResponseEntity<Void> pauseJob(@PathVariable UUID id) {
        cronJobService.pauseJob(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/jobs/{id}/resume")
    public ResponseEntity<Void> resumeJob(@PathVariable UUID id) {
        cronJobService.resumeJob(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        cronJobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/jobs/{id}/trigger")
    public ResponseEntity<CronJobExecution> triggerManual(@PathVariable UUID id) {
        return ResponseEntity.ok(cronJobService.triggerManual(id));
    }

    @GetMapping("/jobs/{id}/executions")
    public ResponseEntity<List<CronJobExecution>> getExecutions(@PathVariable UUID id) {
        return ResponseEntity.ok(cronJobService.getExecutionHistory(id));
    }

    @PostMapping("/dags")
    public ResponseEntity<CronDAG> createDAG(@Valid @RequestBody DAGRequest request) {
        return ResponseEntity.ok(cronJobService.createDAG(request));
    }
}
