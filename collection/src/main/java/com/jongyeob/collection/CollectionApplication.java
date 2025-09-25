package com.jongyeob.collection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.jongyeob.collection.config.CollectorProperties;

@SpringBootApplication
@EnableConfigurationProperties(CollectorProperties.class)
@EnableScheduling
public class CollectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollectionApplication.class, args);
    }
}
