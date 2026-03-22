package net.devstudy.resume.staticdata.internal.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import net.devstudy.resume.shared.middleware.http.PlatformRestClientCustomizer;

@Configuration
@ConditionalOnProperty(name = "app.services.staticdata.mode", havingValue = "remote")
public class StaticDataClientConfig {

    @Bean
    public RestClient staticDataRestClient(
            @Value("${app.services.staticdata.base-url:http://localhost:8084}") String baseUrl,
            PlatformRestClientCustomizer platformRestClientCustomizer) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl);
        return platformRestClientCustomizer.apply(builder, "staticdata-service").build();
    }
}
