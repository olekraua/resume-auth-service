package net.devstudy.resume.shared.middleware.http;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;

public class CircuitBreakingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final String clientName;
    private final PlatformMiddlewareProperties.Circuit circuitProperties;
    private final PlatformMiddlewareProperties.Telemetry telemetryProperties;
    private final PlatformCircuitBreakerRegistry circuitBreakerRegistry;
    private final MeterRegistry meterRegistry;

    public CircuitBreakingClientHttpRequestInterceptor(String clientName,
                                                       PlatformMiddlewareProperties middlewareProperties,
                                                       PlatformCircuitBreakerRegistry circuitBreakerRegistry,
                                                       MeterRegistry meterRegistry) {
        this.clientName = normalizeClientName(clientName);
        this.circuitProperties = middlewareProperties.getCircuit();
        this.telemetryProperties = middlewareProperties.getTelemetry();
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (!circuitProperties.isEnabled()) {
            return execution.execute(request, body);
        }
        String method = resolveMethod(request);
        if (!isMethodSupported(method)) {
            return execution.execute(request, body);
        }
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.getOrCreate(clientName);
        if (!circuitBreaker.tryAcquirePermission()) {
            incrementMetric(circuitBreaker, method, "short_circuited");
            throw CallNotPermittedException.createCallNotPermittedException(circuitBreaker);
        }
        long startedAt = System.nanoTime();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            int statusCode = response.getStatusCode().value();
            if (isFailureStatus(statusCode)) {
                circuitBreaker.onError(
                        elapsedNanos(startedAt),
                        TimeUnit.NANOSECONDS,
                        new HttpStatusFailureException(statusCode));
                incrementMetric(circuitBreaker, method, "failure_status");
            } else {
                circuitBreaker.onSuccess(elapsedNanos(startedAt), TimeUnit.NANOSECONDS);
                incrementMetric(circuitBreaker, method, "success");
            }
            return response;
        } catch (IOException ex) {
            circuitBreaker.onError(elapsedNanos(startedAt), TimeUnit.NANOSECONDS, ex);
            incrementMetric(circuitBreaker, method, "failure_exception");
            throw ex;
        } catch (RuntimeException ex) {
            circuitBreaker.onError(elapsedNanos(startedAt), TimeUnit.NANOSECONDS, ex);
            incrementMetric(circuitBreaker, method, "failure_exception");
            throw ex;
        }
    }

    private String resolveMethod(HttpRequest request) {
        if (request.getMethod() == null) {
            return "UNKNOWN";
        }
        return request.getMethod().name().toUpperCase(Locale.ROOT);
    }

    private boolean isMethodSupported(String method) {
        return circuitProperties.getAllowedMethods().contains(method);
    }

    private boolean isFailureStatus(int statusCode) {
        return circuitProperties.getFailureStatusCodes().contains(statusCode);
    }

    private long elapsedNanos(long startedAt) {
        return Math.max(0L, System.nanoTime() - startedAt);
    }

    private void incrementMetric(CircuitBreaker circuitBreaker, String method, String outcome) {
        if (!telemetryProperties.isEnabled() || meterRegistry == null) {
            return;
        }
        meterRegistry.counter(
                telemetryProperties.getCircuitBreakerMetricName(),
                "client", clientName,
                "method", method,
                "outcome", outcome,
                "state", circuitBreaker.getState().name())
                .increment();
    }

    private String normalizeClientName(String clientName) {
        if (clientName == null || clientName.isBlank()) {
            return "default-client";
        }
        return clientName.trim().toLowerCase(Locale.ROOT);
    }

    private static final class HttpStatusFailureException extends RuntimeException {

        private HttpStatusFailureException(int statusCode) {
            super("Failure status for circuit breaker: " + statusCode);
        }
    }
}
