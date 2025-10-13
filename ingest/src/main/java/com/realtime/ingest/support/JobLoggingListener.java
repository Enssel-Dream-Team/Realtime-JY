package com.realtime.ingest.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Component
public class JobLoggingListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobLoggingListener.class);

    @Override
    public void afterJob(JobExecution jobExecution) {
        long total = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
        long processed = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
        long filtered = jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getFilterCount).sum();
        long failed = jobExecution.getAllFailureExceptions().size();

        log.info(
            "Job {} completed with status {} - total: {}, processed: {}, filtered: {}, failed: {}",
            jobExecution.getJobInstance().getJobName(),
            jobExecution.getStatus(),
            total,
            processed,
            filtered,
            failed
        );
        if (!jobExecution.getAllFailureExceptions().isEmpty()) {
            jobExecution.getAllFailureExceptions()
                .forEach(ex -> log.warn("Failure during job {}: {}", jobExecution.getJobInstance().getJobName(), ex.getMessage(), ex));
        }
    }
}
