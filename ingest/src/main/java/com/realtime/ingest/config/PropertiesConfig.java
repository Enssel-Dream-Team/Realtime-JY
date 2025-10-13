package com.realtime.ingest.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    RssFeedProperties.class,
    WikiDumpProperties.class,
    YoutubeProperties.class
})
public class PropertiesConfig {
}
