package com.realtime.ingest.app.rss;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.realtime.ingest.domain.RawDoc;
import com.realtime.ingest.domain.SourceType;

@Service
public class RssArticleFetchService {

    private static final Logger log = LoggerFactory.getLogger(RssArticleFetchService.class);
    private static final String USER_AGENT = "RealtimeIngest/1.0";
    private static final String ACCEPT_LANGUAGE = "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7";
    private static final int REQUEST_TIMEOUT_MILLIS = (int) Duration.ofSeconds(10).toMillis();

    public Optional<RawDoc> fetchArticle(RssFeedEntry entry) {
        try {
            if (entry.link() == null || entry.link().isBlank()) {
                log.warn("RSS entry {} has no link. Skipping fetch.", entry.sourceId());
                return Optional.empty();
            }
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
}
