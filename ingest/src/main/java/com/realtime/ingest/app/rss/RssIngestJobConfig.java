package com.realtime.ingest.app.rss;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.realtime.ingest.domain.RawDoc;
import com.realtime.ingest.support.JobLoggingListener;

@Configuration
public class RssIngestJobConfig {

    @Bean
    public Step rssIngestStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        RssItemReader reader,
        RssItemProcessor processor,
        RssItemWriter writer
    ) {
        return new StepBuilder("rssIngestStep", jobRepository)
            .<RssFeedEntry, RawDoc>chunk(16, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipPolicy((throwable, skipCount) -> skipCount < 3)
            .build();
    }

    @Bean
    public Job rssIngestJob(JobRepository jobRepository, Step rssIngestStep, JobLoggingListener jobLoggingListener) {
        return new JobBuilder("rssIngestJob", jobRepository)
            .start(rssIngestStep)
            .listener(jobLoggingListener)
            .build();
    }
}
