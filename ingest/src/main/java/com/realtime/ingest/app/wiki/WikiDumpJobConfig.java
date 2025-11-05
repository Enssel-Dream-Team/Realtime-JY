package com.realtime.ingest.app.wiki;

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
public class WikiDumpJobConfig {

    @Bean
    public ItemStreamReader<WikiPage> wikiDumpReader(WikiDumpItemReader delegate) {
        SynchronizedItemStreamReader<WikiPage> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(delegate);
        return reader;
    }

    @Bean
    public Step wikiDumpStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        TaskExecutor ingestTaskExecutor,
        ItemStreamReader<WikiPage> wikiDumpReader,
        WikiDumpItemProcessor processor,
        WikiDumpItemWriter writer
    ) {
        return new StepBuilder("wikiDumpStep", jobRepository)
            .<WikiPage, RawDoc>chunk(8, transactionManager)
            .reader(wikiDumpReader)
            .processor(processor)
            .writer(writer)
            .taskExecutor(ingestTaskExecutor)
            .faultTolerant()
            .skipPolicy((throwable, skipCount) -> skipCount < 3)
            .build();
    }

    @Bean
    public Job wikiDumpJob(JobRepository jobRepository, Step wikiDumpStep, JobLoggingListener jobLoggingListener) {
        return new JobBuilder("wikiDumpJob", jobRepository)
            .start(wikiDumpStep)
            .listener(jobLoggingListener)
            .build();
    }
}
