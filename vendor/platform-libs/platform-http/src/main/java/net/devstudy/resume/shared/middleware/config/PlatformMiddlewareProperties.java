package net.devstudy.resume.shared.middleware.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.platform.middleware")
public class PlatformMiddlewareProperties {

    private final Timeout timeout = new Timeout();
    private final Retry retry = new Retry();
    private final Circuit circuit = new Circuit();
    private final Idempotency idempotency = new Idempotency();
    private final Telemetry telemetry = new Telemetry();

    public Timeout getTimeout() {
        return timeout;
    }

    public Retry getRetry() {
        return retry;
    }

    public Circuit getCircuit() {
        return circuit;
    }

    public Idempotency getIdempotency() {
        return idempotency;
    }

    public Telemetry getTelemetry() {
        return telemetry;
    }

    public static final class Timeout {

        private boolean enabled = true;
        private Duration connect = Duration.ofSeconds(2);
        private Duration read = Duration.ofSeconds(10);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getConnect() {
            return connect;
        }

        public void setConnect(Duration connect) {
            this.connect = sanitizeDuration(connect, Duration.ofSeconds(2));
        }

        public Duration getRead() {
            return read;
        }

        public void setRead(Duration read) {
            this.read = sanitizeDuration(read, Duration.ofSeconds(10));
        }
    }

    public static final class Retry {

        private boolean enabled = true;
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofMillis(100);
        private Duration maxDelay = Duration.ofSeconds(2);
        private double multiplier = 2.0;
        private boolean jitterEnabled = true;
        private double jitterFactor = 0.2;
        private List<Integer> retryableStatusCodes = new ArrayList<>(
                List.of(408, 429, 500, 502, 503, 504));
        private List<String> allowedMethods = new ArrayList<>(
                List.of("GET", "HEAD", "OPTIONS"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = Math.max(1, maxAttempts);
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(Duration initialDelay) {
            this.initialDelay = sanitizeDuration(initialDelay, Duration.ofMillis(100));
        }

        public Duration getMaxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(Duration maxDelay) {
            this.maxDelay = sanitizeDuration(maxDelay, Duration.ofSeconds(2));
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier <= 1.0 ? 1.0 : multiplier;
        }

        public boolean isJitterEnabled() {
            return jitterEnabled;
        }

        public void setJitterEnabled(boolean jitterEnabled) {
            this.jitterEnabled = jitterEnabled;
        }

        public double getJitterFactor() {
            return jitterFactor;
        }

        public void setJitterFactor(double jitterFactor) {
            if (jitterFactor < 0) {
                this.jitterFactor = 0.0;
            } else {
                this.jitterFactor = Math.min(1.0, jitterFactor);
            }
        }

        public List<Integer> getRetryableStatusCodes() {
            return retryableStatusCodes;
        }

        public void setRetryableStatusCodes(List<Integer> retryableStatusCodes) {
            if (retryableStatusCodes == null || retryableStatusCodes.isEmpty()) {
                this.retryableStatusCodes = new ArrayList<>();
                return;
            }
            Set<Integer> unique = new LinkedHashSet<>();
            for (Integer statusCode : retryableStatusCodes) {
                if (statusCode != null && statusCode >= 100 && statusCode <= 599) {
                    unique.add(statusCode);
                }
            }
            this.retryableStatusCodes = new ArrayList<>(unique);
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = normalizeMethods(allowedMethods, List.of("GET", "HEAD", "OPTIONS"));
        }
    }

    public static final class Idempotency {

        private boolean enabled = true;
        private String headerName = "Idempotency-Key";
        private String replayHeaderName = "X-Idempotency-Replay";
        private boolean requireHeader = false;
        private Duration ttl = Duration.ofMinutes(10);
        private int maxEntries = 10_000;
        private int maxBodyBytes = 65_536;
        private boolean includeQueryString = true;
        private List<String> methods = new ArrayList<>(List.of("POST", "PUT", "PATCH"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = defaultIfBlank(headerName, "Idempotency-Key");
        }

        public String getReplayHeaderName() {
            return replayHeaderName;
        }

        public void setReplayHeaderName(String replayHeaderName) {
            this.replayHeaderName = defaultIfBlank(replayHeaderName, "X-Idempotency-Replay");
        }

        public boolean isRequireHeader() {
            return requireHeader;
        }

        public void setRequireHeader(boolean requireHeader) {
            this.requireHeader = requireHeader;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = sanitizeDuration(ttl, Duration.ofMinutes(10));
        }

        public int getMaxEntries() {
            return maxEntries;
        }

        public void setMaxEntries(int maxEntries) {
            this.maxEntries = Math.max(1, maxEntries);
        }

        public int getMaxBodyBytes() {
            return maxBodyBytes;
        }

        public void setMaxBodyBytes(int maxBodyBytes) {
            this.maxBodyBytes = Math.max(0, maxBodyBytes);
        }

        public boolean isIncludeQueryString() {
            return includeQueryString;
        }

        public void setIncludeQueryString(boolean includeQueryString) {
            this.includeQueryString = includeQueryString;
        }

        public List<String> getMethods() {
            return methods;
        }

        public void setMethods(List<String> methods) {
            this.methods = normalizeMethods(methods, List.of("POST", "PUT", "PATCH"));
        }
    }

    public static final class Circuit {

        private boolean enabled = true;
        private int slidingWindowSize = 20;
        private int minimumNumberOfCalls = 10;
        private double failureRateThreshold = 50.0;
        private int permittedCallsInHalfOpenState = 5;
        private Duration openStateWait = Duration.ofSeconds(30);
        private boolean automaticTransitionFromOpenToHalfOpen = true;
        private List<Integer> failureStatusCodes = new ArrayList<>(
                List.of(500, 502, 503, 504));
        private List<String> allowedMethods = new ArrayList<>(
                List.of("GET", "HEAD", "OPTIONS", "POST", "PUT", "PATCH", "DELETE"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public void setSlidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = Math.max(2, slidingWindowSize);
        }

        public int getMinimumNumberOfCalls() {
            return minimumNumberOfCalls;
        }

        public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
            this.minimumNumberOfCalls = Math.max(1, minimumNumberOfCalls);
        }

        public double getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(double failureRateThreshold) {
            if (failureRateThreshold <= 0.0) {
                this.failureRateThreshold = 1.0;
            } else {
                this.failureRateThreshold = Math.min(100.0, failureRateThreshold);
            }
        }

        public int getPermittedCallsInHalfOpenState() {
            return permittedCallsInHalfOpenState;
        }

        public void setPermittedCallsInHalfOpenState(int permittedCallsInHalfOpenState) {
            this.permittedCallsInHalfOpenState = Math.max(1, permittedCallsInHalfOpenState);
        }

        public Duration getOpenStateWait() {
            return openStateWait;
        }

        public void setOpenStateWait(Duration openStateWait) {
            this.openStateWait = sanitizeDuration(openStateWait, Duration.ofSeconds(30));
        }

        public boolean isAutomaticTransitionFromOpenToHalfOpen() {
            return automaticTransitionFromOpenToHalfOpen;
        }

        public void setAutomaticTransitionFromOpenToHalfOpen(boolean automaticTransitionFromOpenToHalfOpen) {
            this.automaticTransitionFromOpenToHalfOpen = automaticTransitionFromOpenToHalfOpen;
        }

        public List<Integer> getFailureStatusCodes() {
            return failureStatusCodes;
        }

        public void setFailureStatusCodes(List<Integer> failureStatusCodes) {
            this.failureStatusCodes = normalizeStatusCodes(
                    failureStatusCodes,
                    List.of(500, 502, 503, 504));
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = normalizeMethods(
                    allowedMethods,
                    List.of("GET", "HEAD", "OPTIONS", "POST", "PUT", "PATCH", "DELETE"));
        }
    }

    public static final class Telemetry {

        private boolean enabled = true;
        private String requestIdHeader = "X-Request-Id";
        private String requestIdMdcKey = "requestId";
        private String traceIdHeader = "X-Trace-Id";
        private boolean propagateRequestId = true;
        private String httpClientMetricName = "resume.platform.http.client.requests";
        private String idempotencyMetricName = "resume.platform.idempotency.requests";
        private String circuitBreakerMetricName = "resume.platform.circuitbreaker.calls";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getRequestIdHeader() {
            return requestIdHeader;
        }

        public void setRequestIdHeader(String requestIdHeader) {
            this.requestIdHeader = defaultIfBlank(requestIdHeader, "X-Request-Id");
        }

        public String getRequestIdMdcKey() {
            return requestIdMdcKey;
        }

        public void setRequestIdMdcKey(String requestIdMdcKey) {
            this.requestIdMdcKey = defaultIfBlank(requestIdMdcKey, "requestId");
        }

        public String getTraceIdHeader() {
            return traceIdHeader;
        }

        public void setTraceIdHeader(String traceIdHeader) {
            this.traceIdHeader = defaultIfBlank(traceIdHeader, "X-Trace-Id");
        }

        public boolean isPropagateRequestId() {
            return propagateRequestId;
        }

        public void setPropagateRequestId(boolean propagateRequestId) {
            this.propagateRequestId = propagateRequestId;
        }

        public String getHttpClientMetricName() {
            return httpClientMetricName;
        }

        public void setHttpClientMetricName(String httpClientMetricName) {
            this.httpClientMetricName = defaultIfBlank(
                    httpClientMetricName,
                    "resume.platform.http.client.requests");
        }

        public String getIdempotencyMetricName() {
            return idempotencyMetricName;
        }

        public void setIdempotencyMetricName(String idempotencyMetricName) {
            this.idempotencyMetricName = defaultIfBlank(
                    idempotencyMetricName,
                    "resume.platform.idempotency.requests");
        }

        public String getCircuitBreakerMetricName() {
            return circuitBreakerMetricName;
        }

        public void setCircuitBreakerMetricName(String circuitBreakerMetricName) {
            this.circuitBreakerMetricName = defaultIfBlank(
                    circuitBreakerMetricName,
                    "resume.platform.circuitbreaker.calls");
        }
    }

    private static List<Integer> normalizeStatusCodes(List<Integer> statusCodes, List<Integer> defaults) {
        if (statusCodes == null || statusCodes.isEmpty()) {
            return new ArrayList<>(defaults);
        }
        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer statusCode : statusCodes) {
            if (statusCode != null && statusCode >= 100 && statusCode <= 599) {
                unique.add(statusCode);
            }
        }
        if (unique.isEmpty()) {
            return new ArrayList<>(defaults);
        }
        return new ArrayList<>(unique);
    }

    private static List<String> normalizeMethods(List<String> methods, List<String> defaults) {
        if (methods == null || methods.isEmpty()) {
            return new ArrayList<>(defaults);
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String method : methods) {
            if (method == null || method.isBlank()) {
                continue;
            }
            normalized.add(method.trim().toUpperCase(Locale.ROOT));
        }
        if (normalized.isEmpty()) {
            return new ArrayList<>(defaults);
        }
        return new ArrayList<>(normalized);
    }

    private static Duration sanitizeDuration(Duration value, Duration defaultValue) {
        if (value == null || value.isNegative() || value.isZero()) {
            return defaultValue;
        }
        return value;
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
