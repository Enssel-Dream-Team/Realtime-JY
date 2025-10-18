package com.realtime.ingest.app.rss;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.realtime.ingest.domain.RawDoc;
import com.realtime.ingest.domain.SourceType;

@Service
public class RssArticleFetchService {

    private static final Logger log = LoggerFactory.getLogger(RssArticleFetchService.class);
    private static final String USER_AGENT = "RealtimeIngest/1.0";
    private static final String ACCEPT_LANGUAGE = "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7";
    private static final int REQUEST_TIMEOUT_MILLIS = (int) Duration.ofSeconds(10).toMillis();
    private static final Pattern JTBC_ARTICLE_PATTERN = Pattern.compile("/article/([A-Z]{2}\\d+)");
    private static final Duration API_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient.Builder webClientBuilder;
    private final String jtbcApiBaseUrl;

    public RssArticleFetchService(
        WebClient.Builder webClientBuilder,
        @Value("${ingest.rss.jtbc-api-base-url:https://news.jtbc.co.kr}") String jtbcApiBaseUrl
    ) {
        this.webClientBuilder = webClientBuilder;
        this.jtbcApiBaseUrl = jtbcApiBaseUrl.endsWith("/")
            ? jtbcApiBaseUrl.substring(0, jtbcApiBaseUrl.length() - 1)
            : jtbcApiBaseUrl;
    }

    public Optional<RawDoc> fetchArticle(RssFeedEntry entry) {
        if (entry.link() == null || entry.link().isBlank()) {
            log.warn("RSS entry {} has no link. Skipping fetch.", entry.sourceId());
            return Optional.empty();
        }

        Optional<RawDoc> jtbcArticle = fetchFromJtbcApi(entry);
        if (jtbcArticle.isPresent()) {
            return jtbcArticle;
        }

        try {
            Document document = Jsoup.connect(entry.link())
                .userAgent(USER_AGENT)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .timeout(REQUEST_TIMEOUT_MILLIS)
                .followRedirects(true)
                .get();
            String title = document.title();
            String bodyText = document.body() != null ? document.body().text() : "";

            RawDoc rawDoc = new RawDoc();
            rawDoc.setSource(SourceType.RSS);
            rawDoc.setSourceId(entry.sourceId());
            rawDoc.setTitle(title != null ? title : entry.title());
            rawDoc.setContent(bodyText);
            rawDoc.setOriginalUrl(entry.link());
            rawDoc.setEventTime(entry.publishedAt() != null ? entry.publishedAt() : Instant.now());
            return Optional.of(rawDoc);
        } catch (IOException e) {
            log.warn("Failed to fetch article {}: {}", entry.link(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<RawDoc> fetchFromJtbcApi(RssFeedEntry entry) {
        URI linkUri;
        try {
            linkUri = URI.create(entry.link());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid article link {}: {}", entry.link(), e.getMessage());
            return Optional.empty();
        }

        if (!"news.jtbc.co.kr".equalsIgnoreCase(linkUri.getHost())) {
            return Optional.empty();
        }

        Matcher matcher = JTBC_ARTICLE_PATTERN.matcher(linkUri.getPath());
        if (!matcher.find()) {
            return Optional.empty();
        }

        String articleId = matcher.group(1);
        String apiUrl = UriComponentsBuilder.fromHttpUrl(jtbcApiBaseUrl)
            .pathSegment("restapi", "v2", "get", "article", articleId)
            .toUriString();

        try {
            JsonNode response = webClientBuilder.build()
                .get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(API_TIMEOUT);

            if (response == null) {
                log.warn("JTBC API returned empty body for {}", articleId);
                return Optional.empty();
            }
            if (!"00".equals(response.path("resultCode").asText())) {
                log.warn("JTBC API responded with non-success code {} for {}", response.path("resultCode").asText(), articleId);
                return Optional.empty();
            }

            JsonNode data = response.path("data");
            String content = data.path("articleInnerTextContent").asText(null);
            if (!StringUtils.hasText(content)) {
                String htmlContent = data.path("articleContent").asText(null);
                if (StringUtils.hasText(htmlContent)) {
                    content = Jsoup.parse(htmlContent).text();
                }
            }
            if (!StringUtils.hasText(content)) {
                log.warn("JTBC API response missing article content for {}", articleId);
                return Optional.empty();
            }

            String title = entry.title();
            if (!StringUtils.hasText(title)) {
                title = data.path("articleTitle").asText(null);
            }

            RawDoc rawDoc = new RawDoc();
            rawDoc.setSource(SourceType.RSS);
            rawDoc.setSourceId(entry.sourceId());
            rawDoc.setTitle(title);
            rawDoc.setContent(content.trim());
            rawDoc.setOriginalUrl(entry.link());
            rawDoc.setEventTime(entry.publishedAt() != null ? entry.publishedAt() : Instant.now());
            return Optional.of(rawDoc);
        } catch (WebClientResponseException e) {
            log.warn("JTBC API responded with status {} for {}: {}", e.getStatusCode(), articleId, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("JTBC API fetch failed for {}: {}", articleId, e.getMessage());
            return Optional.empty();
        }
    }
}
