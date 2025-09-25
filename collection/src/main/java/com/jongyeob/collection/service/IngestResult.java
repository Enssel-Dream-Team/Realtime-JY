package com.jongyeob.collection.service;

public record IngestResult(
    String dedupKey,
    String canonicalUrl,
    boolean canonicalized
) {
}
