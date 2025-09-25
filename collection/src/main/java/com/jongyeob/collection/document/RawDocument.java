package com.jongyeob.collection.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = RawDocument.COLLECTION_NAME)
public record RawDocument(
    @Id String id,
    String source,
    String canonicalUrl,
    String originUrl,
    Instant eventTime,
    Instant ingestTime,
    String title,
    String body
) {
    public static final String COLLECTION_NAME = "raw_docs";
}
