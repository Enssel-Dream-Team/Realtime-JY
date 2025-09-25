package com.jongyeob.collection.rss;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.jongyeob.collection.canonical.CanonicalUrlNormalizer;
import com.jongyeob.collection.canonical.CanonicalizedUrl;
import com.jongyeob.collection.config.CollectorProperties;
import com.jongyeob.collection.document.RawDocumentRepository;
import com.jongyeob.collection.service.CollectorService;
import com.jongyeob.collection.service.DedupKeyGenerator;
import com.jongyeob.collection.service.IngestCommand;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

@Component
public class RssFeedCollector {

    private static final Logger log = LoggerFactory.getLogger(RssFeedCollector.class);

    private final CollectorProperties properties;
    private final RestTemplate restTemplate;
    private final CollectorService collectorService;
    private final RawDocumentRepository rawDocumentRepository;
    private final CanonicalUrlNormalizer canonicalUrlNormalizer;
    private final DedupKeyGenerator dedupKeyGenerator;

    public RssFeedCollector(
        CollectorProperties properties,
        RestTemplate restTemplate,
        CollectorService collectorService,
        RawDocumentRepository rawDocumentRepository,
        CanonicalUrlNormalizer canonicalUrlNormalizer,
        DedupKeyGenerator dedupKeyGenerator
    ) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.collectorService = collectorService;
        this.rawDocumentRepository = rawDocumentRepository;
        this.canonicalUrlNormalizer = canonicalUrlNormalizer;
        this.dedupKeyGenerator = dedupKeyGenerator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Initial RSS poll triggered at application startup");
        pollFeeds();
    }

    @Scheduled(fixedDelayString = "${collector.rss.poll-interval:PT5M}")
    public void pollFeeds() {
        List<CollectorProperties.Rss.Publisher> publishers = properties.getRss().getPublishers();
        if (publishers.isEmpty()) {
            log.debug("No RSS publishers configured, skipping poll");
            return;
        }

        for (CollectorProperties.Rss.Publisher publisher : publishers) {
            fetchPublisher(publisher);
        }
    }

    private void fetchPublisher(CollectorProperties.Rss.Publisher publisher) {
        String url = publisher.getUrl();
        if (!StringUtils.hasText(url)) {
            log.warn("Publisher {} has no URL configured, skipping", publisher.getId());
            return;
        }

        log.info("Polling RSS feed for publisher={} url={} source={}", publisher.getId(), url, publisher.getSource());
        try {
            restTemplate.execute(url, HttpMethod.GET, null, response -> {
                try (InputStream body = response.getBody()) {
                    if (body == null) {
                        log.warn("Empty response body for publisher {}", publisher.getId());
                        return null;
                    }
                    parseFeed(publisher, body);
                } catch (FeedException | IOException ex) {
                    log.error("Failed to parse RSS feed for publisher {}", publisher.getId(), ex);
                }
                return null;
            });
        } catch (ResourceAccessException ex) {
            log.warn("Failed to fetch RSS feed for publisher {}", publisher.getId(), ex);
        }
    }

    private void parseFeed(CollectorProperties.Rss.Publisher publisher, InputStream body) throws FeedException, IOException {
        SyndFeedInput input = new SyndFeedInput();
        try (XmlReader reader = new XmlReader(body)) {
            SyndFeed feed = input.build(reader);
            int totalEntries = 0;
            int ingestedEntries = 0;
            int duplicateEntries = 0;
            int skippedEntries = 0;
            for (SyndEntry entry : feed.getEntries()) {
                EntryProcessingResult result = processEntry(publisher, entry);
                switch (result) {
                    case INGESTED -> {
                        totalEntries++;
                        ingestedEntries++;
                    }
                    case DUPLICATE -> {
                        totalEntries++;
                        duplicateEntries++;
                    }
                    case SKIPPED -> skippedEntries++;
                }
            }
            log.info(
                "RSS summary publisher={} totalArticles={} ingested={} duplicates={} skipped={}",
                publisher.getId(),
                totalEntries,
                ingestedEntries,
                duplicateEntries,
                skippedEntries
            );
        }
    }

    private EntryProcessingResult processEntry(CollectorProperties.Rss.Publisher publisher, SyndEntry entry) {
        String link = entry.getLink();
        if (!StringUtils.hasText(link)) {
            log.debug("Skipping entry without link for publisher {}", publisher.getId());
            return EntryProcessingResult.SKIPPED;
        }

        String source = resolveSource(publisher);
        CanonicalizedUrl canonical = canonicalUrlNormalizer.normalize(link);
        String canonicalUrl = canonical.value();
        String dedupKey = dedupKeyGenerator.generate(source, canonicalUrl);

        if (rawDocumentRepository.existsById(dedupKey)) {
            log.debug("Document already processed source={} canonicalUrl={}", source, canonicalUrl);
            return EntryProcessingResult.DUPLICATE;
        }

        log.info("Collecting new RSS entry source={} canonicalUrl={}", source, canonicalUrl);
        Instant eventTime = firstNonNull(entry.getPublishedDate(), entry.getUpdatedDate())
            .map(java.util.Date::toInstant)
            .orElse(null);

        String title = entry.getTitle();
        String body = extractBody(entry);

        IngestCommand command = new IngestCommand(
            source,
            link,
            eventTime,
            title,
            body
        );

        collectorService.ingest(command);
        return EntryProcessingResult.INGESTED;
    }

    private enum EntryProcessingResult {
        INGESTED,
        DUPLICATE,
        SKIPPED
    }

    private String resolveSource(CollectorProperties.Rss.Publisher publisher) {
        if (StringUtils.hasText(publisher.getSource())) {
            return publisher.getSource();
        }
        if (StringUtils.hasText(publisher.getId())) {
            return publisher.getId();
        }
        throw new IllegalStateException("Publisher must define either source or id");
    }

    private Optional<java.util.Date> firstNonNull(java.util.Date first, java.util.Date second) {
        if (first != null) {
            return Optional.of(first);
        }
        return Optional.ofNullable(second);
    }

    private String extractBody(SyndEntry entry) {
        if (entry.getContents() != null && !entry.getContents().isEmpty()) {
            return entry.getContents().stream()
                .map(SyndContent::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
        }
        if (entry.getDescription() != null) {
            return entry.getDescription().getValue();
        }
        return null;
    }
}
