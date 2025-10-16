package com.realtime.ingest.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.realtime.ingest.config.IngestKafkaTopicsProperties;
import com.realtime.ingest.domain.IngestRawDocMessage;
import com.realtime.ingest.domain.RawDoc;
import com.realtime.ingest.domain.SourceType;
import com.realtime.ingest.repository.RawDocRepository;
import com.realtime.ingest.support.CanonicalUrlUtil;
import com.realtime.ingest.support.DedupKeyService;

@Service
public class RawDocService {

    private static final Logger log = LoggerFactory.getLogger(RawDocService.class);

    private final RawDocRepository repository;
    private final DedupKeyService dedupKeyService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IngestKafkaTopicsProperties kafkaTopics;

    public RawDocService(
        RawDocRepository repository,
        DedupKeyService dedupKeyService,
        KafkaTemplate<String, Object> kafkaTemplate,
        IngestKafkaTopicsProperties kafkaTopics
    ) {
        this.repository = repository;
        this.dedupKeyService = dedupKeyService;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopics = kafkaTopics;
    }

    public RawDocSaveResult storeRawDoc(RawDoc rawDoc) {
        String canonicalUrl = CanonicalUrlUtil.canonicalize(rawDoc.getOriginalUrl());
        rawDoc.setCanonicalUrl(canonicalUrl);
        String dedupKey = dedupKeyService.dedupKey(rawDoc.getSource(), rawDoc.getSourceId(), canonicalUrl);
        Optional<RawDoc> existing = repository.findByCanonicalUrl(canonicalUrl);
        if (existing.isPresent()) {
            log.info("Skip duplicated doc {} for URL {}", rawDoc.getSourceId(), canonicalUrl);
            return RawDocSaveResult.skipped(dedupKey, "duplicated");
        }
        rawDoc.setId(dedupKey);
        rawDoc.setIngestTime(Instant.now());
        repository.save(rawDoc);
        publishMessage(rawDoc, dedupKey);
        return RawDocSaveResult.stored(dedupKey);
    }

    private void publishMessage(RawDoc rawDoc, String dedupKey) {
        SourceType sourceType = rawDoc.getSource();
        IngestRawDocMessage.MongoReference mongoReference =
            new IngestRawDocMessage.MongoReference("realtime", "raw_docs", rawDoc.getId());
        String topic = kafkaTopics.topicFor(sourceType);
        IngestRawDocMessage message = new IngestRawDocMessage(
            dedupKey,
            sourceType,
            rawDoc.getSourceId(),
            Optional.ofNullable(rawDoc.getEventTime()).orElse(Instant.now()),
            rawDoc.getIngestTime(),
            UUID.randomUUID().toString(),
            mongoReference
        );
        try {
            kafkaTemplate.send(topic, dedupKey, message).get(5, TimeUnit.SECONDS);
            log.debug("Sent message for raw doc {} to {} topic {}", dedupKey, sourceType, topic);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka 전송 중 인터럽트가 발생했습니다.", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Kafka 전송에 실패했습니다.", e);
        }
    }
}
