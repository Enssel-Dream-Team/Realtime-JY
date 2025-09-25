package com.jongyeob.collection.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MongoDocumentReference(
    String db,
    String collection,
    @JsonProperty("_id") String id
) {
}
