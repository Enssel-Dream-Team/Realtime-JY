package com.realtime.ingest.domain;

import java.time.Instant;
import java.util.Objects;

public class IngestRawDocMessage {

    private String dedupKey;
    private SourceType source;
    private String sourceId;
    private Instant eventTime;
    private Instant ingestTime;
    private String traceId;
    private MongoReference mongoRef;

    public IngestRawDocMessage() {
    }

    public IngestRawDocMessage(
        String dedupKey,
        SourceType source,
        String sourceId,
        Instant eventTime,
        Instant ingestTime,
        String traceId,
        MongoReference mongoRef
    ) {
        this.dedupKey = dedupKey;
        this.source = source;
        this.sourceId = sourceId;
        this.eventTime = eventTime;
        this.ingestTime = ingestTime;
        this.traceId = traceId;
        this.mongoRef = mongoRef;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
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

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public MongoReference getMongoRef() {
        return mongoRef;
    }

    public void setMongoRef(MongoReference mongoRef) {
        this.mongoRef = mongoRef;
    }

    public static class MongoReference {
        private String db;
        private String collection;
        private String id;

        public MongoReference() {
        }

        public MongoReference(String db, String collection, String id) {
            this.db = db;
            this.collection = collection;
            this.id = id;
        }

        public String getDb() {
            return db;
        }

        public void setDb(String db) {
            this.db = db;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MongoReference that)) return false;
            return Objects.equals(db, that.db)
                && Objects.equals(collection, that.collection)
                && Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(db, collection, id);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IngestRawDocMessage that)) return false;
        return Objects.equals(dedupKey, that.dedupKey)
            && source == that.source
            && Objects.equals(sourceId, that.sourceId)
            && Objects.equals(eventTime, that.eventTime)
            && Objects.equals(ingestTime, that.ingestTime)
            && Objects.equals(traceId, that.traceId)
            && Objects.equals(mongoRef, that.mongoRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dedupKey, source, sourceId, eventTime, ingestTime, traceId, mongoRef);
    }
}
