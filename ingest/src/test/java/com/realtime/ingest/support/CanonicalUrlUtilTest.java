package com.realtime.ingest.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CanonicalUrlUtilTest {

    @Test
    @DisplayName("utm 파라미터와 대소문자를 정규화한다")
    void canonicalize_removesTrackingParams() {
        String canonical = CanonicalUrlUtil.canonicalize("HTTPS://example.com/news?id=1&utm_source=test&gclid=abc");
        assertThat(canonical).isEqualTo("https://example.com/news/?id=1");
    }

    @Test
    @DisplayName("해시 프래그먼트를 제거한다")
    void canonicalize_stripsFragment() {
        String canonical = CanonicalUrlUtil.canonicalize("https://example.com/path#section");
        assertThat(canonical).isEqualTo("https://example.com/path/");
    }
}
