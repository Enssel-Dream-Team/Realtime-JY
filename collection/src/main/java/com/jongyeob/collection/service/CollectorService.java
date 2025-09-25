package com.jongyeob.collection.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jongyeob.collection.canonical.CanonicalUrlNormalizer;
import com.jongyeob.collection.canonical.CanonicalizedUrl;
import com.jongyeob.collection.config.CollectorProperties;
import com.jongyeob.collection.document.RawDocument;
import com.jongyeob.collection.document.RawDocumentRepository;
import com.jongyeob.collection.messaging.kafka.MongoDocumentReference;
import com.jongyeob.collection.messaging.kafka.RawDocumentKafkaAdapter;
import com.jongyeob.collection.messaging.kafka.RawIngestEvent;

@Service
public class CollectorService {

    private static final Logger log = LoggerFactory.getLogger(CollectorService.class);

    private final CanonicalUrlNormalizer canonicalUrlNormalizer;
    private final RawDocumentRepository rawDocumentRepository;
    private final DedupKeyGenerator dedupKeyGenerator;
    private final RawDocumentKafkaAdapter kafkaAdapter;
    private final CollectorProperties collectorProperties;
    private final Clock clock;

    public CollectorService(
        CanonicalUrlNormalizer canonicalUrlNormalizer,
        RawDocumentRepository rawDocumentRepository,
        DedupKeyGenerator dedupKeyGenerator,
        RawDocumentKafkaAdapter kafkaAdapter,
        CollectorProperties collectorProperties,
        Clock clock
    ) {
        this.canonicalUrlNormalizer = canonicalUrlNormalizer;
        this.rawDocumentRepository = rawDocumentRepository;
        this.dedupKeyGenerator = dedupKeyGenerator;
        this.kafkaAdapter = kafkaAdapter;
        this.collectorProperties = collectorProperties;
        this.clock = clock;
    }

    public IngestResult ingest(IngestCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        String source = requireNonBlank(command.source(), "source");
        String originUrl = requireNonBlank(command.originUrl(), "originUrl");

        CanonicalizedUrl canonical = canonicalUrlNormalizer.normalize(originUrl);
        String canonicalUrl = canonical.value();
        String dedupKey = dedupKeyGenerator.generate(source, canonicalUrl);

        Instant ingestTime = clock.instant();
        Instant eventTime = command.eventTime() != null ? command.eventTime() : ingestTime;
        String normalizedSource = source.trim().toLowerCase(Locale.ROOT);

        RawDocument document = new RawDocument(
            dedupKey,
            normalizedSource,
            canonicalUrl,
            originUrl,
            eventTime,
            ingestTime,
            command.title(),
            command.body()
        );

        RawDocument saved = rawDocumentRepository.save(document);
        log.debug("Saved document {} for source {}", saved.id(), saved.source());

        MongoDocumentReference mongoRef = new MongoDocumentReference(
            collectorProperties.getMongo().getDatabase(),
            collectorProperties.getMongo().getCollection(),
            saved.id()
        );

        RawIngestEvent event = new RawIngestEvent(
            saved.id(),
            saved.source(),
            saved.eventTime(),
            saved.ingestTime(),
            saved.title(),
            saved.originUrl(),
            mongoRef
        );

        kafkaAdapter.publish(event);
        return new IngestResult(saved.id(), canonicalUrl, canonical.normalized());
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
