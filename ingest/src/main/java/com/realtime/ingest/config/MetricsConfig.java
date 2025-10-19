package com.realtime.ingest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;

@Configuration
public class MetricsConfig {

    private static final String BATCH_JOB_ACTIVE_METRIC = "spring.batch.job.active";
    private static final String STATUS_TAG = "spring.batch.job.status";
    private static final String DEFAULT_STATUS = "UNKNOWN";

    @Bean
    public MeterFilter springBatchJobActiveStatusFilter() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if (BATCH_JOB_ACTIVE_METRIC.equals(id.getName()) && id.getTag(STATUS_TAG) == null) {
                    return id.withTags(Tags.concat(id.getTagsAsIterable(), Tags.of(STATUS_TAG, DEFAULT_STATUS)));
                }
                return id;
            }
        };
    }
}
