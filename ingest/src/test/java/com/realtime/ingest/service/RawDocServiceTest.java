package com.realtime.ingest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.realtime.ingest.domain.RawDoc;
import com.realtime.ingest.domain.SourceType;
import com.realtime.ingest.repository.RawDocRepository;
import com.realtime.ingest.support.DedupKeyService;

@ExtendWith(MockitoExtension.class)
class RawDocServiceTest {

    @Mock
    private RawDocRepository repository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private RawDocService rawDocService;

    @BeforeEach
    void setUp() {
        rawDocService = new RawDocService(repository, new DedupKeyService(), kafkaTemplate, "topic");
    }

    @Test
    void storeRawDoc_savesAndPublishesMessage() {
        when(repository.findByCanonicalUrl(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.complete(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        RawDoc doc = new RawDoc();
        doc.setSource(SourceType.RSS);
        doc.setSourceId("yonhap-1");
        doc.setOriginalUrl("https://example.com/news?id=1&utm_source=test");
        doc.setTitle("example");
        doc.setContent("content");
        doc.setEventTime(Instant.parse("2024-01-01T00:00:00Z"));

        RawDocSaveResult result = rawDocService.storeRawDoc(doc);

        assertThat(result.isSaved()).isTrue();
        verify(repository).save(any(RawDoc.class));
        verify(kafkaTemplate).send(eq("topic"), anyString(), any());
    }

    @Test
    void storeRawDoc_skipsDuplicates() {
        RawDoc existing = new RawDoc();
        existing.setId("dup");
        when(repository.findByCanonicalUrl(any())).thenReturn(Optional.of(existing));

        RawDoc doc = new RawDoc();
        doc.setSource(SourceType.RSS);
        doc.setSourceId("yonhap-1");
        doc.setOriginalUrl("https://example.com/news?id=1");

        RawDocSaveResult result = rawDocService.storeRawDoc(doc);

        assertThat(result.isSaved()).isFalse();
        verify(repository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void storeRawDoc_throwsWhenKafkaSendFails() {
        when(repository.findByCanonicalUrl(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("kafka down"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        RawDoc doc = new RawDoc();
        doc.setSource(SourceType.RSS);
        doc.setSourceId("yonhap-1");
        doc.setOriginalUrl("https://example.com/news?id=1");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> rawDocService.storeRawDoc(doc))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Kafka 전송에 실패했습니다.");
    }
}
