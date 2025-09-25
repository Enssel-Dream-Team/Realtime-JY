package com.jongyeob.collection.web.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngestRequest(
    @NotBlank String source,
    @NotBlank String originUrl,
    Instant eventTime,
    @Size(max = 1024) String title,
    @Size(max = 131072) String body
) {
}
