package com.realtime.ingest.service;

public class RawDocSaveResult {
    private final boolean saved;
    private final String dedupKey;
    private final String reason;

    private RawDocSaveResult(boolean saved, String dedupKey, String reason) {
        this.saved = saved;
        this.dedupKey = dedupKey;
        this.reason = reason;
    }

    public static RawDocSaveResult stored(String dedupKey) {
        return new RawDocSaveResult(true, dedupKey, null);
    }

    public static RawDocSaveResult skipped(String dedupKey, String reason) {
        return new RawDocSaveResult(false, dedupKey, reason);
    }

    public boolean isSaved() {
        return saved;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public String getReason() {
        return reason;
    }
}
