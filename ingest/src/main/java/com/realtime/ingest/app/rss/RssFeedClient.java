package com.realtime.ingest.app.rss;

import java.io.StringReader;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;

@Component
public class RssFeedClient {

    private static final Logger log = LoggerFactory.getLogger(RssFeedClient.class);

    private final WebClient webClient;

    public RssFeedClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public Optional<RssFeedResponse> fetch(String url, String lastEtag, Instant lastModified) {
        try {
            WebClient.RequestHeadersSpec<?> request = webClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, "application/rss+xml, application/xml");
            if (lastEtag != null) {
                request.header(HttpHeaders.IF_NONE_MATCH, lastEtag);
            }
            if (lastModified != null) {
                String httpDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(lastModified.atOffset(ZoneOffset.UTC));
                request.header(HttpHeaders.IF_MODIFIED_SINCE, httpDate);
            }

            ResponseEntity<String> response = request.exchangeToMono(clientResponse -> clientResponse.toEntity(String.class)).block();

            if (response == null
                || response.getStatusCode().is3xxRedirection()
                || response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()) {
                log.warn("Failed to read feed {} status {}", url, response != null ? response.getStatusCode() : "null");
                return Optional.empty();
            }

            if (response.getStatusCode().value() == 304) {
                log.info("Feed {} not modified", url);
                return Optional.empty();
            }

            SyndFeedInput input = new SyndFeedInput();
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            SyndFeed feed = input.build(new StringReader(body));
            String etag = response.getHeaders().getETag();
            Instant modified = response.getHeaders().getLastModified() > 0
                ? Instant.ofEpochMilli(response.getHeaders().getLastModified())
                : null;

            return Optional.of(new RssFeedResponse(feed, etag, modified));
        } catch (Exception e) {
            log.warn("Failed to fetch feed {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    public record RssFeedResponse(SyndFeed feed, String etag, Instant lastModified) {
    }
}
