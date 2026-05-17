package org.sudhir512kj.cronjob.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.cronjob.dto.CronJobRequest;
import org.sudhir512kj.cronjob.dto.CronJobResponse;
import org.sudhir512kj.cronjob.dto.DAGRequest;
import org.sudhir512kj.cronjob.engine.CronSchedulerEngine;
import org.sudhir512kj.cronjob.model.CronDAG;
import org.sudhir512kj.cronjob.model.CronJob;
import org.sudhir512kj.cronjob.model.CronJobExecution;
import org.sudhir512kj.cronjob.repository.CronDAGRepository;
import org.sudhir512kj.cronjob.repository.CronJobExecutionRepository;
import org.sudhir512kj.cronjob.repository.CronJobRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CronJobService {
    private final CronJobRepository jobRepository;
    private final CronDAGRepository dagRepository;
    private final CronJobExecutionRepository executionRepository;
    private final CronSchedulerEngine schedulerEngine;
    private final ObjectMapper objectMapper;

    @Transactional
    public CronJobResponse createJob(CronJobRequest request) {
        validateCronExpression(request.getCronExpression());

        CronJob job = new CronJob();
        job.setName(request.getName());
        job.setNamespace(request.getNamespace());
        job.setCronExpression(request.getCronExpression());
        job.setTimezone(request.getTimezone());
        job.setTaskType(request.getTaskType());
        job.setTimeoutSeconds(request.getTimeoutSeconds());
        job.setMaxRetries(request.getMaxRetries());
        job.setRetryDelaySeconds(request.getRetryDelaySeconds());
        job.setConcurrencyPolicy(request.getConcurrencyPolicy());
        job.setDagId(request.getDagId());
        job.setDescription(request.getDescription());

        try {
            if (request.getTaskConfig() != null)
                job.setTaskConfig(objectMapper.writeValueAsString(request.getTaskConfig()));
            if (request.getDependsOn() != null)
                job.setDependsOn(objectMapper.writeValueAsString(request.getDependsOn()));
            if (request.getLabels() != null)
                job.setLabels(objectMapper.writeValueAsString(request.getLabels()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize config", e);
        }

        // Calculate first run
        LocalDateTime nextRun = schedulerEngine.calculateNextRun(request.getCronExpression(), request.getTimezone());
        job.setNextRunAt(nextRun);

        job = jobRepository.save(job);
        log.info("Created cron job: {} in namespace {}", job.getName(), job.getNamespace());
        return CronJobResponse.from(job);
    }

    @Transactional
    public CronDAG createDAG(DAGRequest request) {
        validateCronExpression(request.getCronExpression());

        CronDAG dag = new CronDAG();
        dag.setName(request.getName());
        dag.setNamespace(request.getNamespace());
        dag.setCronExpression(request.getCronExpression());
        dag.setTimezone(request.getTimezone());
        dag.setMaxParallelTasks(request.getMaxParallelTasks());
        dag.setDagTimeoutSeconds(request.getDagTimeoutSeconds());
        dag.setDescription(request.getDescription());

        return dagRepository.save(dag);
    }

    public CronJobResponse getJob(UUID id) {
        return CronJobResponse.from(jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found: " + id)));
    }

    public List<CronJobResponse> listJobs(String namespace) {
        return jobRepository.findByNamespace(namespace).stream()
                .map(CronJobResponse::from).toList();
    }

    @Transactional
    public void pauseJob(UUID id) {
        jobRepository.updateStatus(id, CronJob.JobStatus.PAUSED);
    }

    @Transactional
    public void resumeJob(UUID id) {
        CronJob job = jobRepository.findById(id).orElseThrow();
        job.setStatus(CronJob.JobStatus.ACTIVE);
        job.setNextRunAt(schedulerEngine.calculateNextRun(job.getCronExpression(), job.getTimezone()));
        jobRepository.save(job);
    }

    @Transactional
    public void deleteJob(UUID id) {
        jobRepository.deleteById(id);
    }

    @Transactional
    public CronJobExecution triggerManual(UUID id) {
        CronJob job = jobRepository.findById(id).orElseThrow();
        CronJobExecution exec = new CronJobExecution();
        exec.setJobId(id);
        exec.setStatus(CronJob.RunStatus.RUNNING);
        exec.setStartedAt(LocalDateTime.now());
        exec.setTriggeredBy("MANUAL");
        return executionRepository.save(exec);
    }

    public List<CronJobExecution> getExecutionHistory(UUID jobId) {
        return executionRepository.findByJobIdOrderByStartedAtDesc(jobId);
    }

    private void validateCronExpression(String expression) {
        try {
            CronExpression.parse(expression);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cron expression: " + expression);
        }
    }
}
