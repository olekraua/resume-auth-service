package net.devstudy.resume.shared.middleware.http;

import java.net.http.HttpClient;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;

public class PlatformClientHttpRequestFactoryProvider {

    private final PlatformMiddlewareProperties middlewareProperties;

    public PlatformClientHttpRequestFactoryProvider(PlatformMiddlewareProperties middlewareProperties) {
        this.middlewareProperties = middlewareProperties;
    }

    public boolean isTimeoutEnabled() {
        return middlewareProperties.getTimeout().isEnabled();
    }

    public ClientHttpRequestFactory createDefaultFactory() {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        applyConnectTimeout(clientBuilder);
        return createFactory(clientBuilder.build());
    }

    public ClientHttpRequestFactory createFactory(HttpClient httpClient) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        if (isTimeoutEnabled()) {
            requestFactory.setReadTimeout(middlewareProperties.getTimeout().getRead());
        }
        return requestFactory;
    }

    public void applyConnectTimeout(HttpClient.Builder clientBuilder) {
        if (!isTimeoutEnabled()) {
            return;
        }
        clientBuilder.connectTimeout(middlewareProperties.getTimeout().getConnect());
    }
}
