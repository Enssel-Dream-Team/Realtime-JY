package com.jongyeob.collection.messaging.kafka;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.jongyeob.collection.config.CollectorProperties;

@Component
public class RawDocumentKafkaAdapter {

    private static final Logger log = LoggerFactory.getLogger(RawDocumentKafkaAdapter.class);

    private final KafkaTemplate<String, RawIngestEvent> kafkaTemplate;
    private final CollectorProperties properties;

    public RawDocumentKafkaAdapter(
        KafkaTemplate<String, RawIngestEvent> kafkaTemplate,
        CollectorProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public CompletableFuture<SendResult<String, RawIngestEvent>> publish(RawIngestEvent event) {
        String topic = properties.getTopic().getRawIngest();
        log.debug("Publishing raw ingest event to topic {} with key {}", topic, event.dedupKey());
        return kafkaTemplate.send(topic, event.dedupKey(), event);
    }
}
