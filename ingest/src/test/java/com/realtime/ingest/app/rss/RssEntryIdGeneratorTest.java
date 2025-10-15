package com.realtime.ingest.app.rss;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.realtime.ingest.config.RssFeedProperties;
import com.rometools.rome.feed.synd.SyndEntryImpl;

class RssEntryIdGeneratorTest {

    private RssEntryIdGenerator generator;
    private RssFeedProperties.Feed feed;

    @BeforeEach
    void setUp() {
        generator = new RssEntryIdGenerator();
        feed = new RssFeedProperties.Feed();
        feed.setId("yonhap");
    }

    @Test
    void sameUriProducesStableId() {
        SyndEntryImpl entry1 = new SyndEntryImpl();
        entry1.setUri("https://example.com/article/123");

        SyndEntryImpl entry2 = new SyndEntryImpl();
        entry2.setUri("https://example.com/article/123");

        String id1 = generator.generate(feed, entry1);
        String id2 = generator.generate(feed, entry2);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1).startsWith("yonhap-");
    }

    @Test
    void fallbackFieldsStillGenerateStableId() {
        SyndEntryImpl entry1 = new SyndEntryImpl();
        entry1.setTitle("Breaking news");
        entry1.setPublishedDate(Date.from(java.time.Instant.parse("2024-11-20T08:00:00Z")));

        SyndEntryImpl entry2 = new SyndEntryImpl();
        entry2.setTitle("Breaking news");
        entry2.setPublishedDate(Date.from(java.time.Instant.parse("2024-11-20T08:00:00Z")));

        String id1 = generator.generate(feed, entry1);
        String id2 = generator.generate(feed, entry2);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1).startsWith("yonhap-");
    }

    @Test
    void distinctEntriesProduceDifferentIds() {
        SyndEntryImpl entry1 = new SyndEntryImpl();
        entry1.setUri("https://example.com/article/abc");

        SyndEntryImpl entry2 = new SyndEntryImpl();
        entry2.setUri("https://example.com/article/def");

        assertThat(generator.generate(feed, entry1))
            .isNotEqualTo(generator.generate(feed, entry2));
    }
}
