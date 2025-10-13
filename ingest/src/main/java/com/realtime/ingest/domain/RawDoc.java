package com.realtime.ingest.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "raw_docs")
public class RawDoc {

    @Id
    private String id;
    private SourceType source;
    private String sourceId;
    private String title;
    private String content;
    private String originalUrl;
    private String canonicalUrl;
    private Instant eventTime;
    private Instant ingestTime;
    private Map<String, Object> metadata = new HashMap<>();

    public RawDoc() {
    }

    public RawDoc(
        String id,
        SourceType source,
        String sourceId,
        String title,
        String content,
        String originalUrl,
        String canonicalUrl,
        Instant eventTime,
        Instant ingestTime,
        Map<String, Object> metadata
    ) {
        this.id = id;
        this.source = source;
        this.sourceId = sourceId;
        this.title = title;
        this.content = content;
        this.originalUrl = originalUrl;
        this.canonicalUrl = canonicalUrl;
        this.eventTime = eventTime;
        this.ingestTime = ingestTime;
        if (metadata != null) {
            this.metadata = metadata;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SourceType getSource() {
        return source;
    }

    public void setSource(SourceType source) {
        this.source = source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public void setCanonicalUrl(String canonicalUrl) {
        this.canonicalUrl = canonicalUrl;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public Instant getIngestTime() {
        return ingestTime;
    }

    public void setIngestTime(Instant ingestTime) {
        this.ingestTime = ingestTime;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
