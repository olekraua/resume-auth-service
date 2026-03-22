package net.devstudy.resume.shared.middleware.http;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;

public class TelemetryClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final String MDC_TRACE_ID_KEY = "traceId";

    private final String clientName;
    private final PlatformMiddlewareProperties.Telemetry telemetryProperties;
    private final MeterRegistry meterRegistry;

    public TelemetryClientHttpRequestInterceptor(String clientName,
                                                 PlatformMiddlewareProperties middlewareProperties,
                                                 MeterRegistry meterRegistry) {
        this.clientName = clientName;
        this.telemetryProperties = middlewareProperties.getTelemetry();
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        maybePropagateTraceId(request);
        maybePropagateRequestId(request);
        long startedAt = System.nanoTime();
        String method = request.getMethod() == null ? "UNKNOWN" : request.getMethod().name();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            int statusCode = response.getStatusCode().value();
            recordRequestMetrics(method, Integer.toString(statusCode), classifyOutcome(statusCode), startedAt);
            return response;
        } catch (IOException ex) {
            recordRequestMetrics(method, "IO_ERROR", "ERROR", startedAt);
            throw ex;
        } catch (RuntimeException ex) {
            recordRequestMetrics(method, "CLIENT_EXCEPTION", "ERROR", startedAt);
            throw ex;
        }
    }

    private void maybePropagateRequestId(HttpRequest request) {
        if (!telemetryProperties.isPropagateRequestId()) {
            return;
        }
        String requestIdHeader = telemetryProperties.getRequestIdHeader();
        String currentRequestId = request.getHeaders().getFirst(requestIdHeader);
        if (StringUtils.hasText(currentRequestId)) {
            return;
        }
        request.getHeaders().set(requestIdHeader, resolveRequestId());
    }

    private void maybePropagateTraceId(HttpRequest request) {
        String traceIdHeader = telemetryProperties.getTraceIdHeader();
        if (!StringUtils.hasText(traceIdHeader)) {
            return;
        }
        String currentTraceId = request.getHeaders().getFirst(traceIdHeader);
        if (StringUtils.hasText(currentTraceId)) {
            return;
        }
        String traceIdFromMdc = MDC.get(MDC_TRACE_ID_KEY);
        if (StringUtils.hasText(traceIdFromMdc)) {
            request.getHeaders().set(traceIdHeader, traceIdFromMdc.trim());
        }
    }

    private String resolveRequestId() {
        String mdcKey = telemetryProperties.getRequestIdMdcKey();
        String requestIdFromMdc = MDC.get(mdcKey);
        if (StringUtils.hasText(requestIdFromMdc)) {
            return requestIdFromMdc;
        }
        return UUID.randomUUID().toString();
    }

    private void recordRequestMetrics(String method, String status, String outcome, long startedAt) {
        if (meterRegistry == null) {
            return;
        }
        Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
        Timer.builder(telemetryProperties.getHttpClientMetricName())
                .tag("client", clientName)
                .tag("method", method)
                .tag("status", status)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(duration);
    }

    private String classifyOutcome(int statusCode) {
        if (statusCode >= 200 && statusCode < 400) {
            return "SUCCESS";
        }
        if (statusCode >= 400 && statusCode < 500) {
            return "CLIENT_ERROR";
        }
        return "SERVER_ERROR";
    }
}
