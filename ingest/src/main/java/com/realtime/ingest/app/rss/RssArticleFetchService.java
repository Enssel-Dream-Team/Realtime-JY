package com.realtime.ingest.app.rss;

import java.io.IOException;
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

    public Optional<RawDoc> fetchArticle(RssFeedEntry entry) {
        try {
            Document document = Jsoup.connect(entry.link()).get();
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
