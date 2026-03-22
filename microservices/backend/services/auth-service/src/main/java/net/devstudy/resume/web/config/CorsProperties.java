package net.devstudy.resume.web.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private static final Pattern LOCAL_ORIGIN_PATTERN = Pattern.compile(
            "^(https?)://(localhost|127\\.0\\.0\\.1)(:\\d+)?$");

    private List<String> allowedOrigins = new ArrayList<>();
    private List<String> allowedMethods = new ArrayList<>(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    private List<String> allowedHeaders = new ArrayList<>(
            List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Requested-With"));
    private List<String> exposedHeaders = new ArrayList<>();
    private boolean allowCredentials = true;
    private Long maxAge = 3600L;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = normalizeAllowedOrigins(allowedOrigins);
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods == null ? new ArrayList<>() : allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders == null ? new ArrayList<>() : allowedHeaders;
    }

    public List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders == null ? new ArrayList<>() : exposedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public Long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(Long maxAge) {
        this.maxAge = maxAge;
    }

    private List<String> normalizeAllowedOrigins(List<String> configuredOrigins) {
        if (configuredOrigins == null || configuredOrigins.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> normalizedOrigins = new LinkedHashSet<>();
        for (String configuredOrigin : configuredOrigins) {
            if (configuredOrigin == null || configuredOrigin.isBlank()) {
                continue;
            }
            for (String maybeDelimitedOrigin : configuredOrigin.split(",")) {
                String origin = maybeDelimitedOrigin.trim();
                if (origin.isEmpty()) {
                    continue;
                }
                normalizedOrigins.add(origin);
                addCompanionLocalOrigin(origin, normalizedOrigins);
            }
        }
        return new ArrayList<>(normalizedOrigins);
    }

    private void addCompanionLocalOrigin(String origin, Set<String> normalizedOrigins) {
        Matcher matcher = LOCAL_ORIGIN_PATTERN.matcher(origin);
        if (!matcher.matches()) {
            return;
        }
        String host = matcher.group(2);
        String port = matcher.group(3) == null ? "" : matcher.group(3);
        normalizedOrigins.add("https://" + host + port);
        normalizedOrigins.add("http://" + host + port);
    }
}
