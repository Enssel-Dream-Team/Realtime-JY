package com.realtime.ingest.app.rss;

import java.time.Instant;

public record RssFeedEntry(
    String feedId,
    String sourceId,
    String title,
    String link,
    Instant publishedAt
) {
}
