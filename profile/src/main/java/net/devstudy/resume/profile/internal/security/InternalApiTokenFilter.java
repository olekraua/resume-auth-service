package net.devstudy.resume.profile.internal.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnProperty(name = "app.internal.api.enabled", havingValue = "true")
public class InternalApiTokenFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Token";

    @Value("${app.internal.api.token:}")
    private String internalToken;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!StringUtils.hasText(internalToken)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Internal API token not configured");
            return;
        }
        String token = request.getHeader(HEADER);
        if (!internalToken.equals(token)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
