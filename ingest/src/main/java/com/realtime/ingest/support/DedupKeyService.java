package com.realtime.ingest.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Component;

import com.realtime.ingest.domain.SourceType;

@Component
public class DedupKeyService {

    private final MessageDigest digest;

    public DedupKeyService() {
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public synchronized String dedupKey(SourceType sourceType, String sourceId, String canonicalUrl) {
        digest.reset();
        String canonicalMaterial = canonicalUrl == null ? "" : canonicalUrl;
        byte[] hash = digest.digest(canonicalMaterial.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        String sourcePrefix = sourceType.name().toLowerCase();
        String sourceIdentifier = sourceId == null ? "" : sourceId;
        return sourcePrefix + "#" + sourceIdentifier + "#" + hex;
    }
}
