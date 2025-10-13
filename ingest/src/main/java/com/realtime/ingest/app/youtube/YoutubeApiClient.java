package com.realtime.ingest.app.youtube;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtime.ingest.config.YoutubeProperties;

@Component
public class YoutubeApiClient {

    private static final Logger log = LoggerFactory.getLogger(YoutubeApiClient.class);

    private final YoutubeProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public YoutubeApiClient(YoutubeProperties properties, WebClient.Builder builder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.webClient = builder.baseUrl("https://www.googleapis.com/youtube/v3").build();
        this.objectMapper = objectMapper;
    }

    public List<YoutubeVideo> fetchTrending() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("YouTube API key is missing. Skipping trending fetch.");
            return List.of();
        }
        try {
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/videos")
                    .queryParam("part", "snippet")
                    .queryParam("chart", "mostPopular")
                    .queryParam("regionCode", properties.getRegionCode())
                    .queryParam("maxResults", properties.getMaxResults())
                    .queryParam("key", properties.getApiKey())
                    .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            if (response == null) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(response);
            List<YoutubeVideo> videos = new ArrayList<>();
            for (JsonNode item : root.path("items")) {
                JsonNode snippet = item.path("snippet");
                videos.add(new YoutubeVideo(
                    item.path("id").asText(),
                    snippet.path("title").asText(),
                    snippet.path("description").asText(),
                    snippet.hasNonNull("publishedAt") ? Instant.parse(snippet.get("publishedAt").asText()) : Instant.now(),
                    snippet.path("channelId").asText()
                ));
            }
            return videos;
        } catch (Exception e) {
            log.error("Failed to fetch trending videos: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
