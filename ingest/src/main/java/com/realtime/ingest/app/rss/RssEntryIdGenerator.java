package com.realtime.ingest.app.rss;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.realtime.ingest.config.RssFeedProperties;
import com.rometools.rome.feed.synd.SyndEntry;

/**
 * RSS 항목별로 안정적인 sourceId를 생성한다.
 */
@Component
public class RssEntryIdGenerator {

    public String generate(RssFeedProperties.Feed feed, SyndEntry entry) {
        Objects.requireNonNull(feed, "feed must not be null");
        Objects.requireNonNull(entry, "entry must not be null");

        String candidate = firstNonBlank(entry.getUri(), entry.getLink());
        if (candidate == null || candidate.isBlank()) {
            candidate = fallbackMaterial(entry);
        }
        if (candidate.isBlank()) {
            candidate = entry.getClass().getName() + "@" + Integer.toHexString(entry.hashCode());
        }
        UUID nameUuid = UUID.nameUUIDFromBytes(candidate.getBytes(StandardCharsets.UTF_8));
        return feed.getId() + "-" + nameUuid;
    }

    private String fallbackMaterial(SyndEntry entry) {
        StringBuilder builder = new StringBuilder();
        if (entry.getTitle() != null && !entry.getTitle().isBlank()) {
            builder.append(entry.getTitle());
        }
        if (entry.getPublishedDate() != null) {
            builder.append("|").append(entry.getPublishedDate().toInstant());
        }
        if (entry.getUpdatedDate() != null) {
            builder.append("|").append(entry.getUpdatedDate().toInstant());
        }
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            builder.append("|").append(entry.getDescription().getValue());
        }
        if (!entry.getModules().isEmpty()) {
            builder.append("|modules=").append(entry.getModules().hashCode());
        }
        return builder.toString();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
