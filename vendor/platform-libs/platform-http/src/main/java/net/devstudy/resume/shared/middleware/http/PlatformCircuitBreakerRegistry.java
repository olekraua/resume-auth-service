package net.devstudy.resume.shared.middleware.http;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;

public class PlatformCircuitBreakerRegistry {

    private final PlatformMiddlewareProperties middlewareProperties;
    private final ConcurrentMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    public PlatformCircuitBreakerRegistry(PlatformMiddlewareProperties middlewareProperties) {
        this.middlewareProperties = Objects.requireNonNull(
                middlewareProperties,
                "middlewareProperties");
    }

    public CircuitBreaker getOrCreate(String clientName) {
        String normalizedClientName = normalizeClientName(clientName);
        return circuitBreakers.computeIfAbsent(normalizedClientName, this::createCircuitBreaker);
    }

    private CircuitBreaker createCircuitBreaker(String clientName) {
        PlatformMiddlewareProperties.Circuit circuit = middlewareProperties.getCircuit();
        int minimumCalls = Math.min(
                circuit.getMinimumNumberOfCalls(),
                circuit.getSlidingWindowSize());
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold((float) circuit.getFailureRateThreshold())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(circuit.getSlidingWindowSize())
                .minimumNumberOfCalls(Math.max(1, minimumCalls))
                .permittedNumberOfCallsInHalfOpenState(circuit.getPermittedCallsInHalfOpenState())
                .waitDurationInOpenState(circuit.getOpenStateWait())
                .automaticTransitionFromOpenToHalfOpenEnabled(
                        circuit.isAutomaticTransitionFromOpenToHalfOpen())
                .build();
        return CircuitBreaker.of(clientName, config);
    }

    private String normalizeClientName(String clientName) {
        if (clientName == null || clientName.isBlank()) {
            return "default-client";
        }
        return clientName.trim().toLowerCase();
    }
}
