package com.jongyeob.collection.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class DedupKeyGenerator {

    private static final String HASH_ALGORITHM = "SHA-256";

    public String generate(String source, String canonicalUrl) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(canonicalUrl, "canonicalUrl must not be null");

        String normalizedSource = source.trim().toLowerCase(Locale.ROOT);
        String normalizedUrl = canonicalUrl.trim();

        byte[] digest = sha256(normalizedUrl);
        String hexDigest = HexFormat.of().formatHex(digest);
        return normalizedSource + "#" + hexDigest;
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            return messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 should always be available", e);
        }
    }
}
