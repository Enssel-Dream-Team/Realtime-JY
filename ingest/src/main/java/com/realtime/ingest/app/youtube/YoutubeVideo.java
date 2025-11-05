package com.realtime.ingest.app.youtube;

import java.time.Instant;

public record YoutubeVideo(
    String videoId,
    String title,
    String description,
    Instant publishedAt,
    String channelId
) {
}
