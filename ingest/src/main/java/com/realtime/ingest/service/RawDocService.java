package com.realtime.ingest.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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
    private final String topic;

    public RawDocService(
        RawDocRepository repository,
        DedupKeyService dedupKeyService,
        KafkaTemplate<String, Object> kafkaTemplate,
        @Value("${ingest.kafka.topic:ingest.cleansing.raw_docs}") String topic
    ) {
        this.repository = repository;
        this.dedupKeyService = dedupKeyService;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
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
        IngestRawDocMessage message = new IngestRawDocMessage(
            dedupKey,
            sourceType,
            rawDoc.getSourceId(),
            Optional.ofNullable(rawDoc.getEventTime()).orElse(Instant.now()),
            rawDoc.getIngestTime(),
            UUID.randomUUID().toString(),
            mongoReference
        );
        kafkaTemplate.send(topic, dedupKey, message);
        log.debug("Sent message for raw doc {} to {}", dedupKey, topic);
    }
}
