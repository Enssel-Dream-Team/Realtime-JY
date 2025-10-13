package com.realtime.ingest.config;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduleConfig {

    private static final Logger log = LoggerFactory.getLogger(JobScheduleConfig.class);

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    public JobScheduleConfig(
        JobLauncher jobLauncher,
        JobRegistry jobRegistry
    ) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
    }

    @Scheduled(cron = "${ingest.schedule.rss-cron:0 0/30 * * * *}")
    public void scheduleRssJob() {
        launchJob("rssIngestJob");
    }

    @Scheduled(cron = "${ingest.schedule.wiki-cron:0 30 2 * * *}")
    public void scheduleWikiJob() {
        launchJob("wikiDumpJob");
    }

    @Scheduled(
        initialDelayString = "${ingest.schedule.youtube-initial-delay:PT1M}",
        fixedDelayString = "${ingest.schedule.youtube-fixed-delay:PT30M}"
    )
    public void scheduleYoutubeJob() {
        log.info("YouTube 수집 작업을 시작합니다");
        launchJob("youtubeIngestJob");
    }

    private void launchJob(String jobName) {
        try {
            Job job = jobRegistry.getJob(jobName);
            JobParameters parameters = new JobParametersBuilder()
                .addLong("launchTime", Instant.now().toEpochMilli())
                .toJobParameters();
            jobLauncher.run(job, parameters);
        } catch (NoSuchJobException e) {
            log.warn("Job {} not registered yet. Skipping.", jobName);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
            log.warn("Job {} could not be launched: {}", jobName, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error launching job {}", jobName, e);
        }
    }
}
