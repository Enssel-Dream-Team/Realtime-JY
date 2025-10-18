package com.realtime.ingest.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {

    private final int maxSize;
    private final int minSize;
    private final Duration maxWaitTime;

    public MongoConfig(
        @Value("${ingest.mongo.pool.max-size:30}") int maxSize,
        @Value("${ingest.mongo.pool.min-size:10}") int minSize,
        @Value("${ingest.mongo.pool.max-wait:PT30S}") Duration maxWaitTime
    ) {
        this.maxSize = maxSize;
        this.minSize = minSize;
        this.maxWaitTime = maxWaitTime;
    }

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoPoolCustomizer() {
        return builder -> builder.applyToConnectionPoolSettings(pool -> pool
            .maxSize(maxSize)
            .minSize(minSize)
            .maxWaitTime(maxWaitTime.toMillis(), TimeUnit.MILLISECONDS));
    }
}
