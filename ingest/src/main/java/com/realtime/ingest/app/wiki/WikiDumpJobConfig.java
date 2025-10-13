package com.realtime.ingest.app.wiki;

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
public class WikiDumpJobConfig {

    @Bean
    public Step wikiDumpStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        WikiDumpItemReader reader,
        WikiDumpItemProcessor processor,
        WikiDumpItemWriter writer
    ) {
        return new StepBuilder("wikiDumpStep", jobRepository)
            .<WikiPage, RawDoc>chunk(8, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
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
