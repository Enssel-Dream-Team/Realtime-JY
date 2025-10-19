package com.realtime.ingest.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class JobShutdownManager {

    private static final Logger log = LoggerFactory.getLogger(JobShutdownManager.class);

    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;

    public JobShutdownManager(JobExplorer jobExplorer, JobOperator jobOperator) {
        this.jobExplorer = jobExplorer;
        this.jobOperator = jobOperator;
    }

    @PreDestroy
    public void stopRunningJobs() {
        for (String jobName : jobExplorer.getJobNames()) {
            Set<JobExecution> runningExecutions = jobExplorer.findRunningJobExecutions(jobName);
            for (JobExecution execution : runningExecutions) {
                Long executionId = execution.getId();
                log.info("애플리케이션 종료 전 {} 실행 {} 중지 시도", jobName, executionId);
                try {
                    jobOperator.stop(executionId);
                } catch (Exception e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof NoSuchJobException) {
                        log.debug("Job registry에 {} 잡이 없어 중지 요청을 건너뜁니다.", jobName);
                    } else {
                        log.warn("{} 실행 {} 중지에 실패했습니다: {}", jobName, executionId, e.getMessage(), e);
                    }
                }
            }
        }
    }
}
