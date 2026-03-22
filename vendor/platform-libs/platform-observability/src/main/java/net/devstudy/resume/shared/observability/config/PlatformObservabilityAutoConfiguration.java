package net.devstudy.resume.shared.observability.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;

import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;
import net.devstudy.resume.shared.middleware.web.RequestIdFilter;

@AutoConfiguration(afterName = "net.devstudy.resume.shared.middleware.config.PlatformMiddlewareAutoConfiguration")
@ConditionalOnClass(OncePerRequestFilter.class)
public class PlatformObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(name = "app.platform.middleware.telemetry.enabled",
            havingValue = "true",
            matchIfMissing = true)
    public RequestIdFilter requestIdFilter(PlatformMiddlewareProperties middlewareProperties) {
        return new RequestIdFilter(middlewareProperties);
    }
}
