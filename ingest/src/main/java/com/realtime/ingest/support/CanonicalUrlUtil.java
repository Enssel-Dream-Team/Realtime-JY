package com.realtime.ingest.support;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class CanonicalUrlUtil {

    private static final Set<String> TRACKING_PARAMS = Set.of(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "gclid", "fbclid"
    );

    private CanonicalUrlUtil() {
    }

    public static String canonicalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return rawUrl;
        }
        try {
            URI uri = new URI(rawUrl);
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            String path = normalizePath(uri.getPath());
            String query = normalizeQuery(uri.getRawQuery());
            return new URI(
                uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(),
                uri.getUserInfo(),
                host,
                uri.getPort(),
                path,
                query,
                null
            ).toString();
        } catch (URISyntaxException e) {
            return rawUrl;
        }
    }

    private static String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "/";
        }
        String normalized = rawPath.endsWith("/") ? rawPath : rawPath + "/";
        return normalized.replaceAll("/+", "/");
    }

    private static String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        TreeMap<String, String> params = Arrays.stream(rawQuery.split("&"))
            .map(param -> param.split("=", 2))
            .filter(pair -> pair.length > 0 && !TRACKING_PARAMS.contains(pair[0].toLowerCase()))
            .collect(Collectors.toMap(
                pair -> pair[0],
                pair -> pair.length > 1 ? pair[1] : "",
                (left, right) -> right,
                TreeMap::new
            ));
        if (params.isEmpty()) {
            return null;
        }
        return params.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&"));
    }
}
