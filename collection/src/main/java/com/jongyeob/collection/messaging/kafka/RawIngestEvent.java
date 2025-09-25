package com.jongyeob.collection.messaging.kafka;

import java.time.Instant;

public record RawIngestEvent(
    String dedupKey,
    String source,
    Instant eventTime,
    Instant ingestTime,
    String title,
    String originUrl,
    MongoDocumentReference mongoRef
) {
}
