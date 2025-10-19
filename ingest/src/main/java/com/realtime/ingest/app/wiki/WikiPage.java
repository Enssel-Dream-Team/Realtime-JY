package com.realtime.ingest.app.wiki;

import java.time.Instant;

public record WikiPage(
    String dumpId,
    String pageId,
    String title,
    String text,
    Instant revisionTimestamp
) {
}
