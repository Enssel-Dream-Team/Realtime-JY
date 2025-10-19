package com.realtime.ingest.app.rss;

import java.util.Optional;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.realtime.ingest.domain.RawDoc;

@Component
public class RssItemProcessor implements ItemProcessor<RssFeedEntry, RawDoc> {

    private final RssArticleFetchService fetchService;

    public RssItemProcessor(RssArticleFetchService fetchService) {
        this.fetchService = fetchService;
    }

    @Override
    public RawDoc process(RssFeedEntry item) {
        Optional<RawDoc> rawDoc = fetchService.fetchArticle(item);
        rawDoc.ifPresent(doc -> doc.getMetadata().put("feedId", item.feedId()));
        return rawDoc.orElse(null);
    }
}
