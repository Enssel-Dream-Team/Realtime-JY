package com.jongyeob.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jongyeob.collection.canonical.CanonicalUrlNormalizer;
import com.jongyeob.collection.config.CollectorProperties;
import com.jongyeob.collection.document.RawDocument;
import com.jongyeob.collection.document.RawDocumentRepository;
import com.jongyeob.collection.messaging.kafka.RawDocumentKafkaAdapter;
import com.jongyeob.collection.messaging.kafka.RawIngestEvent;

@ExtendWith(MockitoExtension.class)
class CollectorServiceTest {

    private final CanonicalUrlNormalizer canonicalUrlNormalizer = new CanonicalUrlNormalizer();
    private final DedupKeyGenerator dedupKeyGenerator = new DedupKeyGenerator();
    private final CollectorProperties properties = new CollectorProperties();
    private final Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private RawDocumentRepository rawDocumentRepository;

    @Mock
    private RawDocumentKafkaAdapter kafkaAdapter;

    private CollectorService collectorService;

    @BeforeEach
    void setUp() {
        collectorService = new CollectorService(
            canonicalUrlNormalizer,
            rawDocumentRepository,
            dedupKeyGenerator,
            kafkaAdapter,
            properties,
            clock
        );
    }

    @Test
    void savesDocumentAndPublishesKafkaEventAfterCanonicalisation() {
        Instant eventTime = Instant.parse("2025-02-10T12:34:56Z");
        IngestCommand command = new IngestCommand(
            "RSS",
            "https://Example.com/articles/123/?utm_source=newsletter&ref=keep",
            eventTime,
            "A headline",
            "Full body"
        );

        when(rawDocumentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaAdapter.publish(any())).thenReturn(CompletableFuture.completedFuture(null));

        IngestResult result = collectorService.ingest(command);

        ArgumentCaptor<RawDocument> documentCaptor = ArgumentCaptor.forClass(RawDocument.class);
        verify(rawDocumentRepository).save(documentCaptor.capture());
        RawDocument stored = documentCaptor.getValue();

        assertThat(stored.id()).isEqualTo(result.dedupKey());
        assertThat(stored.source()).isEqualTo("rss");
        assertThat(stored.canonicalUrl()).isEqualTo("https://example.com/articles/123?ref=keep");
        assertThat(stored.originUrl()).isEqualTo(command.originUrl());
        assertThat(stored.eventTime()).isEqualTo(eventTime);
        assertThat(stored.ingestTime()).isEqualTo(clock.instant());

        ArgumentCaptor<RawIngestEvent> eventCaptor = ArgumentCaptor.forClass(RawIngestEvent.class);
        verify(kafkaAdapter).publish(eventCaptor.capture());
        RawIngestEvent kafkaEvent = eventCaptor.getValue();

        assertThat(kafkaEvent.dedupKey()).isEqualTo(result.dedupKey());
        assertThat(kafkaEvent.mongoRef().db()).isEqualTo(properties.getMongo().getDatabase());
        assertThat(kafkaEvent.mongoRef().collection()).isEqualTo(properties.getMongo().getCollection());
        assertThat(kafkaEvent.originUrl()).isEqualTo(command.originUrl());

        assertThat(result.canonicalized()).isTrue();
        assertThat(result.canonicalUrl()).isEqualTo("https://example.com/articles/123?ref=keep");
    }

    @Test
    void fallsBackToOriginalUrlWhenCanonicalisationFails() {
        IngestCommand command = new IngestCommand(
            "rss",
            "notaurl",
            null,
            null,
            null
        );

        when(rawDocumentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaAdapter.publish(any())).thenReturn(CompletableFuture.completedFuture(null));

        IngestResult result = collectorService.ingest(command);

        assertThat(result.canonicalized()).isFalse();
        assertThat(result.canonicalUrl()).isEqualTo("notaurl");
    }
}
