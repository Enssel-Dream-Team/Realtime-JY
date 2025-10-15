package com.realtime.ingest.app.rss;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.realtime.ingest.domain.RawDoc;

class RssArticleFetchServiceTest {

    private final RssArticleFetchService service = new RssArticleFetchService();

    @Test
    void skipWhenLinkIsMissing() {
        RssFeedEntry entry = new RssFeedEntry("feed", "feed-1", "test", null, Instant.now());

        Optional<RawDoc> result = service.fetchArticle(entry);

        assertThat(result).isEmpty();
    }
}
