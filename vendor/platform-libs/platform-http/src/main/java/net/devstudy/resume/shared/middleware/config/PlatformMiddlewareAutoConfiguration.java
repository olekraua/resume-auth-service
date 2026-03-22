package net.devstudy.resume.shared.middleware.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import net.devstudy.resume.shared.middleware.http.PlatformCircuitBreakerRegistry;
import net.devstudy.resume.shared.middleware.http.PlatformClientHttpRequestFactoryProvider;
import net.devstudy.resume.shared.middleware.http.PlatformRestClientCustomizer;
import net.devstudy.resume.shared.middleware.web.IdempotencyFilter;
import net.devstudy.resume.shared.middleware.web.InMemoryIdempotencyStore;

@AutoConfiguration
@ConditionalOnClass(OncePerRequestFilter.class)
@EnableConfigurationProperties(PlatformMiddlewareProperties.class)
public class PlatformMiddlewareAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PlatformClientHttpRequestFactoryProvider platformClientHttpRequestFactoryProvider(
            PlatformMiddlewareProperties middlewareProperties) {
        return new PlatformClientHttpRequestFactoryProvider(middlewareProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public PlatformCircuitBreakerRegistry platformCircuitBreakerRegistry(
            PlatformMiddlewareProperties middlewareProperties) {
        return new PlatformCircuitBreakerRegistry(middlewareProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public PlatformRestClientCustomizer platformRestClientCustomizer(
            PlatformMiddlewareProperties middlewareProperties,
            PlatformClientHttpRequestFactoryProvider requestFactoryProvider,
            PlatformCircuitBreakerRegistry circuitBreakerRegistry,
            ObjectProvider<MeterRegistry> meterRegistryProvider,
            ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        return new PlatformRestClientCustomizer(
                middlewareProperties,
                requestFactoryProvider,
                circuitBreakerRegistry,
                meterRegistryProvider,
                observationRegistryProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(name = "app.platform.middleware.idempotency.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public InMemoryIdempotencyStore inMemoryIdempotencyStore(
            PlatformMiddlewareProperties middlewareProperties) {
        return new InMemoryIdempotencyStore(middlewareProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(name = "app.platform.middleware.idempotency.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public IdempotencyFilter idempotencyFilter(
            PlatformMiddlewareProperties middlewareProperties,
            InMemoryIdempotencyStore idempotencyStore,
            ObjectProvider<MeterRegistry> meterRegistryProvider) {
        return new IdempotencyFilter(
                middlewareProperties,
                idempotencyStore,
                meterRegistryProvider);
    }
}
