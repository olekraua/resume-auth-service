package net.devstudy.resume.shared.middleware.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;

@ExtendWith(MockitoExtension.class)
class RetryingClientHttpRequestInterceptorTest {

    @Mock
    private ClientHttpRequestExecution execution;

    private PlatformMiddlewareProperties middlewareProperties;
    private RetryingClientHttpRequestInterceptor interceptor;

    @BeforeEach
    void setUp() {
        middlewareProperties = new PlatformMiddlewareProperties();
        middlewareProperties.getRetry().setJitterEnabled(false);
        middlewareProperties.getRetry().setInitialDelay(Duration.ofMillis(1));
        middlewareProperties.getRetry().setMaxDelay(Duration.ofMillis(2));
        interceptor = new RetryingClientHttpRequestInterceptor(middlewareProperties);
    }

    @Test
    void shouldRetryRetryableStatusCodeForSafeMethods() throws IOException {
        HttpRequest request = createRequest(HttpMethod.GET);
        ClientHttpResponse first = new MockClientHttpResponse(new byte[0], HttpStatus.SERVICE_UNAVAILABLE);
        ClientHttpResponse second = new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(first, second);

        ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(execution, times(2)).execute(any(HttpRequest.class), any(byte[].class));
    }

    @Test
    void shouldNotRetryStatusCodeForUnsafeMethodsByDefault() throws IOException {
        HttpRequest request = createRequest(HttpMethod.POST);
        ClientHttpResponse first = new MockClientHttpResponse(new byte[0], HttpStatus.SERVICE_UNAVAILABLE);
        when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(first);

        ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verify(execution, times(1)).execute(any(HttpRequest.class), any(byte[].class));
    }

    @Test
    void shouldRetryIoException() throws IOException {
        HttpRequest request = createRequest(HttpMethod.GET);
        middlewareProperties.getRetry().setMaxAttempts(2);
        ClientHttpResponse second = new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        when(execution.execute(any(HttpRequest.class), any(byte[].class)))
                .thenThrow(new IOException("network problem"))
                .thenReturn(second);

        ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(execution, times(2)).execute(any(HttpRequest.class), any(byte[].class));
    }

    private HttpRequest createRequest(HttpMethod method) {
        HttpHeaders headers = new HttpHeaders();
        Map<String, Object> attributes = new HashMap<>();
        return new HttpRequest() {
            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }

            @Override
            public HttpMethod getMethod() {
                return method;
            }

            @Override
            public URI getURI() {
                return URI.create("http://localhost/test");
            }

            @Override
            public Map<String, Object> getAttributes() {
                return attributes;
            }
        };
    }
}
