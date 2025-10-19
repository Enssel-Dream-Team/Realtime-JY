package com.realtime.ingest.config;

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class WebClientConfig {

    private static final String USER_AGENT = "RealtimeIngest/1.0";

    @Bean
    public WebClientCustomizer realtimeUserAgentCustomizer() {
        return builder -> builder.defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT);
    }
}
