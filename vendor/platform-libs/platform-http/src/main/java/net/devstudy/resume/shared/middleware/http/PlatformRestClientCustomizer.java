package net.devstudy.resume.shared.middleware.http;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.RestClient;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;

public class PlatformRestClientCustomizer {

    private final PlatformMiddlewareProperties middlewareProperties;
    private final PlatformClientHttpRequestFactoryProvider requestFactoryProvider;
    private final PlatformCircuitBreakerRegistry circuitBreakerRegistry;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final ObjectProvider<ObservationRegistry> observationRegistryProvider;

    public PlatformRestClientCustomizer(PlatformMiddlewareProperties middlewareProperties,
                                        PlatformClientHttpRequestFactoryProvider requestFactoryProvider,
                                        PlatformCircuitBreakerRegistry circuitBreakerRegistry,
                                        ObjectProvider<MeterRegistry> meterRegistryProvider,
                                        ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        this.middlewareProperties = middlewareProperties;
        this.requestFactoryProvider = requestFactoryProvider;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.meterRegistryProvider = meterRegistryProvider;
        this.observationRegistryProvider = observationRegistryProvider;
    }

    public RestClient.Builder apply(RestClient.Builder builder, String clientName) {
        if (requestFactoryProvider.isTimeoutEnabled()) {
            builder.requestFactory(requestFactoryProvider.createDefaultFactory());
        }
        if (middlewareProperties.getTelemetry().isEnabled()) {
            ObservationRegistry observationRegistry = observationRegistryProvider.getIfAvailable();
            if (observationRegistry != null) {
                builder.observationRegistry(observationRegistry);
            }
        }
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (middlewareProperties.getCircuit().isEnabled()) {
            builder.requestInterceptor(new CircuitBreakingClientHttpRequestInterceptor(
                    clientName,
                    middlewareProperties,
                    circuitBreakerRegistry,
                    meterRegistry));
        }
        if (middlewareProperties.getRetry().isEnabled()) {
            builder.requestInterceptor(new RetryingClientHttpRequestInterceptor(middlewareProperties));
        }
        if (middlewareProperties.getTelemetry().isEnabled()) {
            builder.requestInterceptor(new TelemetryClientHttpRequestInterceptor(
                    clientName,
                    middlewareProperties,
                    meterRegistry));
        }
        return builder;
    }
}
