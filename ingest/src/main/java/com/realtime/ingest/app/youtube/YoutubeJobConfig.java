package com.realtime.ingest.app.youtube;

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
public class YoutubeJobConfig {

    @Bean
    public Step youtubeStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        YoutubeItemReader reader,
        YoutubeItemProcessor processor,
        YoutubeItemWriter writer
    ) {
        return new StepBuilder("youtubeStep", jobRepository)
            .<YoutubeVideo, RawDoc>chunk(16, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipPolicy((throwable, skipCount) -> skipCount < 3)
            .build();
    }

    @Bean
    public Job youtubeJob(JobRepository jobRepository, Step youtubeStep, JobLoggingListener jobLoggingListener) {
        return new JobBuilder("youtubeJob", jobRepository)
            .start(youtubeStep)
            .listener(jobLoggingListener)
            .build();
    }
}
