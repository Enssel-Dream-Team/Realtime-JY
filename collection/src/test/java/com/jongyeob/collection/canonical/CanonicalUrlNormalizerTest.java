package com.jongyeob.collection.canonical;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CanonicalUrlNormalizerTest {

    private CanonicalUrlNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new CanonicalUrlNormalizer();
    }

    @Test
    void normalizesHostFragmentAndTrackingParameters() {
        CanonicalizedUrl result = normalizer.normalize("HTTPS://Example.com/path/?utm_source=newsletter&ref=keep&gclid=abc123#section");

        assertThat(result.normalized()).isTrue();
        assertThat(result.value()).isEqualTo("https://example.com/path?ref=keep");
    }

    @Test
    void removesTrailingSlashButKeepsNonTrackingParams() {
        CanonicalizedUrl result = normalizer.normalize("https://example.com/foo/?a=1&utm_medium=email&b=2");

        assertThat(result.value()).isEqualTo("https://example.com/foo?a=1&b=2");
    }

    @Test
    void fallsBackToOriginalWhenUrlInvalid() {
        CanonicalizedUrl result = normalizer.normalize("not-a-url");

        assertThat(result.normalized()).isFalse();
        assertThat(result.value()).isEqualTo("not-a-url");
    }
}
