package org.sudhir512kj.cronjob.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.sudhir512kj.cronjob.model.CronJob;
import org.sudhir512kj.cronjob.model.CronJobExecution;
import org.sudhir512kj.cronjob.repository.CronJobExecutionRepository;
import org.sudhir512kj.cronjob.repository.CronJobRepository;
import org.sudhir512kj.cronjob.service.DistributedLockService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CronSchedulerEngine {
    private final CronJobRepository jobRepository;
    private final CronJobExecutionRepository executionRepository;
    private final DistributedLockService lockService;
    private final TaskExecutor taskExecutor;

    private final String nodeId = UUID.randomUUID().toString();

    @Scheduled(fixedDelay = 1000) // Tick every second
    public void tick() {
        List<CronJob> dueJobs = jobRepository.findJobsDueForExecution(LocalDateTime.now());
        for (CronJob job : dueJobs) {
            processJob(job);
        }
    }

    private void processJob(CronJob job) {
        String lockKey = "cron:" + job.getId();
        Optional<Long> token = lockService.tryAcquireLock(lockKey, nodeId);
        if (token.isEmpty()) return; // Another node is handling this

        try {
            // Concurrency policy check
            if (job.getConcurrencyPolicy() == CronJob.ConcurrencyPolicy.FORBID) {
                List<CronJobExecution> running = executionRepository.findRunningExecutions(job.getId());
                if (!running.isEmpty()) {
                    recordSkipped(job);
                    advanceNextRun(job);
                    return;
                }
            }

            // Execute
            CronJobExecution execution = startExecution(job, token.get());
            TaskExecutor.TaskResult result = taskExecutor.execute(job);
            completeExecution(execution, result);

            // Update job stats
            if (result.success()) {
                job.setLastRunStatus(CronJob.RunStatus.SUCCESS);
                job.setSuccessCount(job.getSuccessCount() + 1);
            } else {
                job.setLastRunStatus(CronJob.RunStatus.FAILED);
                job.setFailureCount(job.getFailureCount() + 1);
            }
            job.setLastRunAt(LocalDateTime.now());
            advanceNextRun(job);
        } finally {
            lockService.releaseLock(lockKey, nodeId);
        }
    }

    private CronJobExecution startExecution(CronJob job, Long fencingToken) {
        CronJobExecution exec = new CronJobExecution();
        exec.setJobId(job.getId());
        exec.setStatus(CronJob.RunStatus.RUNNING);
        exec.setStartedAt(LocalDateTime.now());
        exec.setExecutorNode(nodeId);
        exec.setFencingToken(fencingToken);
        exec.setTriggeredBy("SCHEDULER");
        return executionRepository.save(exec);
    }

    private void completeExecution(CronJobExecution exec, TaskExecutor.TaskResult result) {
        exec.setCompletedAt(LocalDateTime.now());
        exec.setDurationMs(java.time.Duration.between(exec.getStartedAt(), exec.getCompletedAt()).toMillis());
        exec.setStatus(result.success() ? CronJob.RunStatus.SUCCESS : CronJob.RunStatus.FAILED);
        exec.setHttpStatusCode(result.statusCode());
        exec.setResponseBody(result.output());
        exec.setErrorMessage(result.error());
        executionRepository.save(exec);
    }

    private void recordSkipped(CronJob job) {
        CronJobExecution exec = new CronJobExecution();
        exec.setJobId(job.getId());
        exec.setStatus(CronJob.RunStatus.SKIPPED);
        exec.setStartedAt(LocalDateTime.now());
        exec.setCompletedAt(LocalDateTime.now());
        exec.setTriggeredBy("SCHEDULER");
        exec.setErrorMessage("Skipped: previous execution still running (FORBID policy)");
        executionRepository.save(exec);
    }

    public void advanceNextRun(CronJob job) {
        LocalDateTime next = calculateNextRun(job.getCronExpression(), job.getTimezone());
        job.setNextRunAt(next);
        jobRepository.save(job);
    }

    public LocalDateTime calculateNextRun(String cronExpression, String timezone) {
        CronExpression cron = CronExpression.parse(cronExpression);
        ZonedDateTime nowInZone = ZonedDateTime.now(ZoneId.of(timezone));
        LocalDateTime nextInZone = cron.next(nowInZone.toLocalDateTime());
        // Convert back to server time
        ZonedDateTime nextZoned = nextInZone.atZone(ZoneId.of(timezone));
        return nextZoned.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
