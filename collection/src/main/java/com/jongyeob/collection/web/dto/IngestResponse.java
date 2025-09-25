package com.jongyeob.collection.web.dto;

public record IngestResponse(
    String dedupKey,
    String canonicalUrl,
    boolean canonicalized
) {
}
