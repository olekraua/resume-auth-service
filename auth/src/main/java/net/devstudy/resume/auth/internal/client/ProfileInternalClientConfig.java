package net.devstudy.resume.auth.internal.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "app.services.profile.mode", havingValue = "remote")
public class ProfileInternalClientConfig {

    @Bean
    public RestClient profileInternalRestClient(
            @Value("${app.services.profile.base-url}") String baseUrl,
            @Value("${app.services.profile.internal-token}") String token) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Token", token)
                .build();
    }
}
