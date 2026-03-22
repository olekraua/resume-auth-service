package net.devstudy.resume.shared.middleware.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
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

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;

@ExtendWith(MockitoExtension.class)
class CircuitBreakingClientHttpRequestInterceptorTest {

    @Mock
    private ClientHttpRequestExecution execution;

    private CircuitBreakingClientHttpRequestInterceptor interceptor;

    @BeforeEach
    void setUp() {
        PlatformMiddlewareProperties middlewareProperties = new PlatformMiddlewareProperties();
        middlewareProperties.getCircuit().setSlidingWindowSize(2);
        middlewareProperties.getCircuit().setMinimumNumberOfCalls(2);
        middlewareProperties.getCircuit().setFailureRateThreshold(50.0);
        middlewareProperties.getCircuit().setOpenStateWait(Duration.ofSeconds(1));
        middlewareProperties.getCircuit().setAllowedMethods(List.of(HttpMethod.GET.name()));
        middlewareProperties.getCircuit().setFailureStatusCodes(List.of(HttpStatus.SERVICE_UNAVAILABLE.value()));
        interceptor = new CircuitBreakingClientHttpRequestInterceptor(
                "profile-service",
                middlewareProperties,
                new PlatformCircuitBreakerRegistry(middlewareProperties),
                null);
    }

    @Test
    void shouldOpenCircuitAfterConsecutiveFailures() throws IOException {
        HttpRequest request = createRequest(HttpMethod.GET);
        ClientHttpResponse failed = new MockClientHttpResponse(new byte[0], HttpStatus.SERVICE_UNAVAILABLE);
        when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(failed, failed);

        ClientHttpResponse first = interceptor.intercept(request, new byte[0], execution);
        ClientHttpResponse second = interceptor.intercept(request, new byte[0], execution);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                .isInstanceOf(CallNotPermittedException.class);
        verify(execution, times(2)).execute(any(HttpRequest.class), any(byte[].class));
    }

    @Test
    void shouldBypassCircuitForUnsupportedMethod() throws IOException {
        HttpRequest request = createRequest(HttpMethod.POST);
        ClientHttpResponse failed = new MockClientHttpResponse(new byte[0], HttpStatus.SERVICE_UNAVAILABLE);
        when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(failed);

        ClientHttpResponse response = interceptor.intercept(request, new byte[0], execution);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verify(execution, times(1)).execute(any(HttpRequest.class), any(byte[].class));
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
