package com.jongyeob.collection.service;

import java.time.Instant;

public record IngestCommand(
    String source,
    String originUrl,
    Instant eventTime,
    String title,
    String body
) {
}
