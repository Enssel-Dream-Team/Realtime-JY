package com.jongyeob.collection.canonical;

public record CanonicalizedUrl(String value, boolean normalized, boolean changed) {

    public static CanonicalizedUrl success(String value, boolean changed) {
        return new CanonicalizedUrl(value, true, changed);
    }

    public static CanonicalizedUrl failure(String value) {
        return new CanonicalizedUrl(value, false, false);
    }
}
