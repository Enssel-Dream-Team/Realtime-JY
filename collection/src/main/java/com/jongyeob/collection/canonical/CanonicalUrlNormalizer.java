package com.jongyeob.collection.canonical;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Applies minimal canonicalisation rules described in the collector design so that
 * downstream systems receive stable URLs for deduplication.
 */
@Component
public class CanonicalUrlNormalizer {

    private static final Set<String> TRACKING_PARAM_EXACT = Set.of("gclid");
    private static final List<String> TRACKING_PARAM_PREFIXES = List.of("utm_");

    public CanonicalizedUrl normalize(String inputUrl) {
        Objects.requireNonNull(inputUrl, "inputUrl must not be null");
        String candidate = inputUrl.trim();
        if (candidate.isEmpty()) {
            return CanonicalizedUrl.failure(inputUrl);
        }

        UriComponents components = toUriComponents(candidate);
        if (components == null) {
            return CanonicalizedUrl.failure(candidate);
        }

        String scheme = components.getScheme();
        String host = components.getHost();
        if (scheme == null || host == null) {
            return CanonicalizedUrl.failure(candidate);
        }

        String normalized = buildCanonicalUrl(
            scheme,
            host,
            components.getPort(),
            components.getPath(),
            components.getQueryParams()
        );

        boolean changed = !normalized.equals(candidate);
        return CanonicalizedUrl.success(normalized, changed);
    }

    private boolean isTrackingParameter(String key) {
        if (TRACKING_PARAM_EXACT.contains(key)) {
            return true;
        }
        for (String prefix : TRACKING_PARAM_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private UriComponents toUriComponents(String candidate) {
        try {
            return UriComponentsBuilder.fromUriString(candidate).build();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String buildCanonicalUrl(
        String scheme,
        String host,
        int port,
        String path,
        MultiValueMap<String, String> queryParams
    ) {
        String normalizedPath = normalizePath(path);
        MultiValueMap<String, String> filteredParams = filterQueryParams(queryParams);

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
            .scheme(scheme)
            .host(host.toLowerCase(Locale.ROOT))
            .path(normalizedPath);

        if (port != -1) {
            builder.port(port);
        }

        filteredParams.forEach((key, values) -> {
            if (values == null || values.isEmpty()) {
                return;
            }
            for (String value : values) {
                if (value != null) {
                    builder.queryParam(key, value);
                }
            }
        });
        return builder.build().toUriString();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private MultiValueMap<String, String> filterQueryParams(MultiValueMap<String, String> queryParams) {
        MultiValueMap<String, String> filtered = new LinkedMultiValueMap<>();
        if (queryParams == null) {
            return filtered;
        }
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (shouldSkipQueryParam(key, values)) {
                continue;
            }
            for (String value : values) {
                if (value != null) {
                    filtered.add(key, value);
                }
            }
        }
        return filtered;
    }

    private boolean shouldSkipQueryParam(String key, List<String> values) {
        if (key == null) {
            return true;
        }
        String lowerKey = key.toLowerCase(Locale.ROOT);
        if (isTrackingParameter(lowerKey)) {
            return true;
        }
        return values == null;
    }
}
