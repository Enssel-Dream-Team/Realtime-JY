package com.realtime.ingest.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.realtime.ingest.support.YamlPropertySourceFactory;

@Configuration
@PropertySource(value = "classpath:feeds.yml", factory = YamlPropertySourceFactory.class)
@PropertySource(value = "classpath:dumps.yml", factory = YamlPropertySourceFactory.class)
@EnableConfigurationProperties({
    RssFeedProperties.class,
    WikiDumpProperties.class,
    YoutubeProperties.class
})
public class PropertiesConfig {
}
