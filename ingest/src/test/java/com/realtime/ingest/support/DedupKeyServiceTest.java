package com.realtime.ingest.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.realtime.ingest.domain.SourceType;

class DedupKeyServiceTest {

    @Test
    void dedupKey_isStableForSameInput() {
        DedupKeyService service = new DedupKeyService();
        String key1 = service.dedupKey(SourceType.RSS, "yonhap-1", "https://example.com");
        String key2 = service.dedupKey(SourceType.RSS, "yonhap-1", "https://example.com");
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void dedupKey_differsBySource() {
        DedupKeyService service = new DedupKeyService();
        String rssKey = service.dedupKey(SourceType.RSS, "id", "https://example.com");
        String ytKey = service.dedupKey(SourceType.YOUTUBE, "id", "https://example.com");
        assertThat(rssKey).isNotEqualTo(ytKey);
    }
}
