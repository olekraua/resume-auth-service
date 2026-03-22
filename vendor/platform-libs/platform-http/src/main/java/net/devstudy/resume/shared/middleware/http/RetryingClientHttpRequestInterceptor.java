package net.devstudy.resume.shared.middleware.http;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;

import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;

public class RetryingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final PlatformMiddlewareProperties.Retry retryProperties;

    public RetryingClientHttpRequestInterceptor(PlatformMiddlewareProperties middlewareProperties) {
        this.retryProperties = middlewareProperties.getRetry();
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (!retryProperties.isEnabled() || retryProperties.getMaxAttempts() <= 1) {
            return execution.execute(request, body);
        }
        if (!isRetryableMethod(resolveMethod(request))) {
            return execution.execute(request, body);
        }
        int attempt = 1;
        Duration nextDelay = retryProperties.getInitialDelay();
        while (true) {
            try {
                ClientHttpResponse response = execution.execute(request, body);
                int statusCode = response.getStatusCode().value();
                if (!canRetryStatus(statusCode, attempt)) {
                    return response;
                }
                response.close();
                sleepBeforeRetry(nextDelay);
                nextDelay = calculateNextDelay(nextDelay);
                attempt++;
            } catch (IOException ex) {
                if (!canRetryException(ex, attempt)) {
                    throw ex;
                }
                sleepBeforeRetry(nextDelay);
                nextDelay = calculateNextDelay(nextDelay);
                attempt++;
            } catch (RuntimeException ex) {
                if (!canRetryException(ex, attempt)) {
                    throw ex;
                }
                sleepBeforeRetry(nextDelay);
                nextDelay = calculateNextDelay(nextDelay);
                attempt++;
            }
        }
    }

    private String resolveMethod(HttpRequest request) {
        if (request.getMethod() == null) {
            return "";
        }
        return request.getMethod().name().toUpperCase(Locale.ROOT);
    }

    private boolean isRetryableMethod(String method) {
        List<String> methods = retryProperties.getAllowedMethods();
        return methods.contains(method);
    }

    private boolean shouldRetryStatus(int statusCode) {
        return retryProperties.getRetryableStatusCodes().contains(statusCode);
    }

    private boolean canRetryStatus(int statusCode, int attempt) {
        return shouldRetryStatus(statusCode) && attempt < retryProperties.getMaxAttempts();
    }

    private boolean shouldRetryException(Throwable throwable) {
        if (throwable instanceof IOException || throwable instanceof ResourceAccessException) {
            return true;
        }
        Throwable cause = throwable.getCause();
        while (cause != null) {
            if (cause instanceof IOException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean canRetryException(Throwable throwable, int attempt) {
        return shouldRetryException(throwable) && attempt < retryProperties.getMaxAttempts();
    }

    private void sleepBeforeRetry(Duration delay) throws IOException {
        long sleepMillis = toSleepMillis(delay);
        if (sleepMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Retry interrupted", ex);
        }
    }

    private long toSleepMillis(Duration delay) {
        long millis = Math.max(0L, delay.toMillis());
        if (!retryProperties.isJitterEnabled() || retryProperties.getJitterFactor() <= 0) {
            return millis;
        }
        double factor = retryProperties.getJitterFactor();
        double random = ThreadLocalRandom.current().nextDouble(-factor, factor);
        long jittered = Math.round(millis * (1.0 + random));
        return Math.max(0L, jittered);
    }

    private Duration calculateNextDelay(Duration currentDelay) {
        long currentDelayMillis = Math.max(0L, currentDelay.toMillis());
        long maxDelayMillis = Math.max(currentDelayMillis, retryProperties.getMaxDelay().toMillis());
        long multiplied = Math.round(currentDelayMillis * retryProperties.getMultiplier());
        long nextMillis = Math.max(currentDelayMillis, multiplied);
        return Duration.ofMillis(Math.min(nextMillis, maxDelayMillis));
    }
}
