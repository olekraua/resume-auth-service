package net.devstudy.resume.ms.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import net.devstudy.resume.ms.auth.adapters.web.config.CorsConfig;
import net.devstudy.resume.ms.auth.adapters.web.config.CorsProperties;
import net.devstudy.resume.ms.auth.adapters.web.controller.api.ApiExceptionHandler;
import net.devstudy.resume.ms.auth.adapters.web.security.CurrentProfileJwtConverter;
import net.devstudy.resume.ms.auth.adapters.web.security.JwtAuthenticationFailureEntryPoint;
import net.devstudy.resume.ms.auth.adapters.web.security.RememberMeSupport;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
@Import({
        CorsConfig.class,
        CurrentProfileJwtConverter.class,
        RememberMeSupport.class,
        JwtAuthenticationFailureEntryPoint.class,
        ApiExceptionHandler.class
})
public class WebModuleWiringConfiguration {
}
