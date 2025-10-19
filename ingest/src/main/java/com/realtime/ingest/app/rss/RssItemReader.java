package com.realtime.ingest.app.rss;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.stereotype.Component;

import com.realtime.ingest.config.IngestProperties;
import com.rometools.rome.feed.synd.SyndEntry;

@Component
public class RssItemReader implements ItemStreamReader<RssFeedEntry>, ItemStream {

    private static final Logger log = LoggerFactory.getLogger(RssItemReader.class);

    private final RssFeedClient feedClient;
    private final IngestProperties ingestProperties;
    private final RssEntryIdGenerator entryIdGenerator;
    private Iterator<RssFeedEntry> iterator;
    private final Map<String, FeedState> runtimeState = new ConcurrentHashMap<>();

    public RssItemReader(
        RssFeedClient feedClient,
        IngestProperties ingestProperties,
        RssEntryIdGenerator entryIdGenerator
    ) {
        this.feedClient = feedClient;
        this.ingestProperties = ingestProperties;
        this.entryIdGenerator = entryIdGenerator;
    }

    @Override
    public RssFeedEntry read() throws Exception, NonTransientResourceException {
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return null;
        }
        return iterator.next();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        List<RssFeedEntry> entries = new ArrayList<>();
        for (IngestProperties.Rss.Feed feed : ingestProperties.getRss().getFeeds()) {
            FeedState previousState = loadState(executionContext, feed.getId());
            feedClient.fetch(feed.getUrl(), previousState.etag(), previousState.lastModified())
                .ifPresent(response -> {
                    runtimeState.put(feed.getId(), new FeedState(response.etag(), response.lastModified()));
                    if (response.feed().getEntries() == null) {
                        return;
                    }
                    for (SyndEntry entry : response.feed().getEntries()) {
                        String sourceId = entryIdGenerator.generate(feed, entry);
                        entries.add(
                            new RssFeedEntry(
                                feed.getId(),
                                sourceId,
                                entry.getTitle(),
                                entry.getLink(),
                                entry.getPublishedDate() != null ? entry.getPublishedDate().toInstant() : Instant.now()
                            )
                        );
                    }
                });
        }
        iterator = entries.iterator();
        log.info("Loaded {} RSS entries for processing", entries.size());
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        runtimeState.forEach((feedId, state) -> {
            if (state.etag() != null) {
                executionContext.putString(stateKey(feedId, "etag"), state.etag());
            }
            if (state.lastModified() != null) {
                executionContext.putLong(stateKey(feedId, "lastModified"), state.lastModified().toEpochMilli());
            }
        });
    }

    @Override
    public void close() throws ItemStreamException {
        iterator = null;
    }

    private FeedState loadState(ExecutionContext context, String feedId) {
        String etagKey = stateKey(feedId, "etag");
        String modifiedKey = stateKey(feedId, "lastModified");
        String etag = context.containsKey(etagKey) ? context.getString(etagKey) : null;
        Instant modified = context.containsKey(modifiedKey)
            ? Instant.ofEpochMilli(context.getLong(modifiedKey))
            : null;
        return new FeedState(etag, modified);
    }

    private String stateKey(String feedId, String suffix) {
        return "feed." + feedId + "." + suffix;
    }

    private record FeedState(String etag, Instant lastModified) {
    }
}
