package com.realtime.ingest.app.rss;

import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import com.realtime.ingest.domain.RawDoc;
import com.realtime.ingest.support.JobLoggingListener;

@Configuration
public class RssIngestJobConfig {

    @Bean
    public ItemStreamReader<RssFeedEntry> rssReader(RssItemReader delegate) {
        SynchronizedItemStreamReader<RssFeedEntry> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(delegate);
        return reader;
    }

    @Bean
    public Step rssStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        TaskExecutor ingestTaskExecutor,
        ItemStreamReader<RssFeedEntry> rssReader,
        RssItemProcessor processor,
        RssItemWriter writer
    ) {
        return new StepBuilder("rssStep", jobRepository)
            .<RssFeedEntry, RawDoc>chunk(16, transactionManager)
            .reader(rssReader)
            .processor(processor)
            .writer(writer)
            .taskExecutor(ingestTaskExecutor)
            .faultTolerant()
            .skipPolicy((throwable, skipCount) -> skipCount < 3)
            .build();
    }

    @Bean
    public Job rssJob(JobRepository jobRepository, Step rssStep, JobLoggingListener jobLoggingListener) {
        return new JobBuilder("rssJob", jobRepository)
            .start(rssStep)
            .listener(jobLoggingListener)
            .build();
    }
}
