package net.devstudy.resume.shared.middleware.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.devstudy.resume.shared.middleware.config.PlatformMiddlewareProperties;

@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestIdFilter extends OncePerRequestFilter {

    private final PlatformMiddlewareProperties middlewareProperties;

    public RequestIdFilter(PlatformMiddlewareProperties middlewareProperties) {
        this.middlewareProperties = middlewareProperties;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestIdHeader = middlewareProperties.getTelemetry().getRequestIdHeader();
        String requestId = request.getHeader(requestIdHeader);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        } else {
            requestId = requestId.trim();
        }
        String requestIdMdcKey = middlewareProperties.getTelemetry().getRequestIdMdcKey();
        MDC.put(requestIdMdcKey, requestId);
        response.setHeader(requestIdHeader, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            maybeSetTraceIdHeader(request, response);
            MDC.remove(requestIdMdcKey);
        }
    }

    private void maybeSetTraceIdHeader(HttpServletRequest request, HttpServletResponse response) {
        String traceIdHeader = middlewareProperties.getTelemetry().getTraceIdHeader();
        if (!StringUtils.hasText(traceIdHeader) || response.containsHeader(traceIdHeader)) {
            return;
        }
        String traceId = MDC.get("traceId");
        if (!StringUtils.hasText(traceId)) {
            traceId = request.getHeader(traceIdHeader);
        }
        if (StringUtils.hasText(traceId)) {
            response.setHeader(traceIdHeader, traceId.trim());
        }
    }
}
