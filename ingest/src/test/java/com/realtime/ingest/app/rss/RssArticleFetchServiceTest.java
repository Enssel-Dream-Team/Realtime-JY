package com.realtime.ingest.app.rss;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.client.WebClient;

import com.realtime.ingest.domain.RawDoc;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class RssArticleFetchServiceTest {

    private MockWebServer mockWebServer;
    private RssArticleFetchService service;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        WebClient.Builder builder = WebClient.builder();
        service = new RssArticleFetchService(builder, mockWebServer.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void fetchArticleUsesJtbcApiWhenAvailable() throws Exception {
        String json = new ClassPathResource("jtbc_article_api_response.json").getContentAsString(StandardCharsets.UTF_8);
        mockWebServer.enqueue(new MockResponse()
            .setBody(json)
            .addHeader("Content-Type", "application/json"));

        RssFeedEntry entry = new RssFeedEntry(
            "jtbc",
            "jtbc-1",
            "정부시스템 복구율 43.6%…국무조정실 통합중계 등 309개 재가동",
            "https://news.jtbc.co.kr/article/NB12266665",
            Instant.parse("2025-10-15T01:47:00Z")
        );

        Optional<RawDoc> rawDoc = service.fetchArticle(entry);

        assertThat(rawDoc).isPresent();
        assertThat(rawDoc.get().getContent()).contains("정부 시스템 709개 가운데 309개가 재가동된");
        assertThat(rawDoc.get().getTitle()).isEqualTo(entry.title());

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/restapi/v2/get/article/NB12266665");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }
}
