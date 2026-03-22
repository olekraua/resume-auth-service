package net.devstudy.resume.shared.middleware.web;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;
import net.devstudy.resume.shared.middleware.web.InMemoryIdempotencyStore.AcquireResult;
import net.devstudy.resume.shared.middleware.web.InMemoryIdempotencyStore.Decision;

@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Set<String> NON_REPLAYABLE_HEADERS = Set.of(
            HttpHeaders.CONTENT_LENGTH.toLowerCase(Locale.ROOT),
            HttpHeaders.TRANSFER_ENCODING.toLowerCase(Locale.ROOT),
            HttpHeaders.CONNECTION.toLowerCase(Locale.ROOT),
            "keep-alive",
            HttpHeaders.UPGRADE.toLowerCase(Locale.ROOT));

    private final PlatformMiddlewareProperties middlewareProperties;
    private final InMemoryIdempotencyStore idempotencyStore;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public IdempotencyFilter(PlatformMiddlewareProperties middlewareProperties,
                             InMemoryIdempotencyStore idempotencyStore,
                             ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.middlewareProperties = middlewareProperties;
        this.idempotencyStore = idempotencyStore;
        this.meterRegistryProvider = meterRegistryProvider;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!middlewareProperties.getIdempotency().isEnabled()) {
            return true;
        }
        String method = request.getMethod();
        if (method == null || !isMethodSupported(method)) {
            return true;
        }
        String idempotencyKey = extractIdempotencyKey(request);
        return !StringUtils.hasText(idempotencyKey) && !middlewareProperties.getIdempotency().isRequireHeader();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String idempotencyKey = extractIdempotencyKey(request);
        if (!StringUtils.hasText(idempotencyKey)) {
            sendMissingKeyResponse(request, response);
            return;
        }
        String cacheKey = buildCacheKey(request, idempotencyKey);
        AcquireResult acquireResult = idempotencyStore.tryAcquire(cacheKey);
        if (acquireResult.getDecision() == Decision.REPLAY) {
            replayCachedResponse(request, response, acquireResult.getCachedResponse());
            return;
        }
        if (acquireResult.getDecision() == Decision.IN_PROGRESS) {
            sendRequestInProgressResponse(request, response);
            return;
        }
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrappedResponse);
            maybeCacheResponse(cacheKey, wrappedResponse);
            incrementMetric("accepted", request.getMethod());
        } catch (ServletException | IOException | RuntimeException ex) {
            idempotencyStore.release(cacheKey);
            throw ex;
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }

    private boolean isMethodSupported(String method) {
        return middlewareProperties.getIdempotency().getMethods().contains(method.toUpperCase(Locale.ROOT));
    }

    private String extractIdempotencyKey(HttpServletRequest request) {
        String headerName = middlewareProperties.getIdempotency().getHeaderName();
        String key = request.getHeader(headerName);
        if (!StringUtils.hasText(key)) {
            return null;
        }
        return key.trim();
    }

    private String buildCacheKey(HttpServletRequest request, String idempotencyKey) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getMethod()).append('|');
        builder.append(request.getRequestURI());
        if (middlewareProperties.getIdempotency().isIncludeQueryString()
                && StringUtils.hasText(request.getQueryString())) {
            builder.append('?').append(request.getQueryString());
        }
        builder.append('|');
        Principal principal = request.getUserPrincipal();
        if (principal != null && StringUtils.hasText(principal.getName())) {
            builder.append(principal.getName().trim());
        } else {
            builder.append("anonymous");
        }
        builder.append('|').append(idempotencyKey);
        return builder.toString();
    }

    private void maybeCacheResponse(String cacheKey, ContentCachingResponseWrapper wrappedResponse) {
        int statusCode = wrappedResponse.getStatus();
        if (statusCode < 200 || statusCode >= 500) {
            idempotencyStore.release(cacheKey);
            return;
        }
        byte[] body = wrappedResponse.getContentAsByteArray();
        if (body.length > middlewareProperties.getIdempotency().getMaxBodyBytes()) {
            idempotencyStore.release(cacheKey);
            return;
        }
        IdempotencyCachedResponse cachedResponse = new IdempotencyCachedResponse(
                statusCode,
                collectHeaders(wrappedResponse),
                body);
        idempotencyStore.complete(cacheKey, cachedResponse);
    }

    private Map<String, List<String>> collectHeaders(ContentCachingResponseWrapper wrappedResponse) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String headerName : wrappedResponse.getHeaderNames()) {
            if (headerName == null || shouldSkipHeader(headerName)) {
                continue;
            }
            headers.put(headerName, new ArrayList<>(wrappedResponse.getHeaders(headerName)));
        }
        return headers;
    }

    private boolean shouldSkipHeader(String headerName) {
        return NON_REPLAYABLE_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private void replayCachedResponse(HttpServletRequest request,
                                      HttpServletResponse response,
                                      IdempotencyCachedResponse cachedResponse) throws IOException {
        if (cachedResponse == null) {
            sendRequestInProgressResponse(request, response);
            return;
        }
        response.setStatus(cachedResponse.status());
        for (Map.Entry<String, List<String>> entry : cachedResponse.headers().entrySet()) {
            String headerName = entry.getKey();
            if (shouldSkipHeader(headerName)) {
                continue;
            }
            for (String value : entry.getValue()) {
                response.addHeader(headerName, value);
            }
        }
        response.setHeader(middlewareProperties.getIdempotency().getReplayHeaderName(), "true");
        byte[] body = cachedResponse.body();
        if (body.length > 0) {
            response.getOutputStream().write(body);
        }
        incrementMetric("replayed", request.getMethod());
    }

    private void sendRequestInProgressResponse(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        incrementMetric("in_progress", request.getMethod());
        response.sendError(
                HttpStatus.CONFLICT.value(),
                "Request with the same idempotency key is already in progress");
    }

    private void sendMissingKeyResponse(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        incrementMetric("missing_key", request.getMethod());
        response.sendError(HttpStatus.BAD_REQUEST.value(),
                "Idempotency key header is required: " + middlewareProperties.getIdempotency().getHeaderName());
    }

    private void incrementMetric(String outcome, String method) {
        if (!middlewareProperties.getTelemetry().isEnabled()) {
            return;
        }
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            return;
        }
        String metricName = middlewareProperties.getTelemetry().getIdempotencyMetricName();
        String safeMethod = method == null ? "UNKNOWN" : method;
        meterRegistry.counter(metricName, "outcome", outcome, "method", safeMethod).increment();
    }
}
