package com.realtime.ingest.config;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduleConfig implements ApplicationListener<ApplicationReadyEvent> {

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

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("ApplicationReadyEvent 수신: 위키 덤프 작업 실행 및 RSS/YouTube 초기 예약을 시작합니다.");
        scheduleInitialRssJob();
        scheduleInitialYoutubeJob();
        launchJob("wikiDumpJob");
    }

    @Scheduled(cron = "${ingest.schedule.rss-cron:0 0/30 * * * *}")
    public void scheduleRssJob() {
        log.info("RSS 수집 작업을 시작합니다.");
        launchJob("rssJob");
    }

    @Scheduled(
        initialDelayString = "${ingest.schedule.youtube-initial-delay:PT1M}",
        fixedDelayString = "${ingest.schedule.youtube-fixed-delay:PT30M}"
    )
    public void scheduleYoutubeJob() {
        log.info("YouTube 수집 작업을 시작합니다.");
        launchJob("youtubeJob");
    }

    private void scheduleInitialRssJob() {
        CompletableFuture
            .delayedExecutor(1, TimeUnit.MINUTES)
            .execute(() -> {
                log.info("애플리케이션 기동 1분 후 RSS 수집 작업을 실행합니다.");
                launchJob("rssJob");
            });
    }

    private void scheduleInitialYoutubeJob() {
        CompletableFuture
            .delayedExecutor(1, TimeUnit.MINUTES)
            .execute(() -> {
                log.info("애플리케이션 기동 1분 후 YouTube 수집 작업을 실행합니다.");
                launchJob("youtubeJob");
            });
    }

    private void launchJob(String jobName) {
        try {
            Job job = jobRegistry.getJob(jobName);
            JobParameters parameters = new JobParametersBuilder()
                .addLong("launchTime", Instant.now().toEpochMilli())
                .toJobParameters();
            jobLauncher.run(job, parameters);
        } catch (NoSuchJobException e) {
            log.warn("{} 잡이 등록되지 않았습니다. 스킵합니다.", jobName);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
            log.warn("Job {} 잡을 시작하지 못했습니다.: {}", jobName, e.getMessage());
        } catch (Exception e) {
            log.error("잡 실행 중 문제가 발생했습니다. {}: {}", jobName, e.getMessage(), e);
        }
    }
}
