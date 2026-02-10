package net.devstudy.resume.web.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import net.devstudy.resume.shared.dto.ApiErrorResponse;

@Component
@ConditionalOnProperty(name = "app.security.jwt.enabled", havingValue = "true")
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException, ServletException {
        LOGGER.info("Access denied: {}", ex.getMessage(), ex);
        String path = resolvePath(request);
        boolean apiRequest = isApiPath(path);
        if (ex instanceof CsrfException) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            if (apiRequest) {
                writeJsonError(response,
                        HttpStatus.UNAUTHORIZED,
                        "CSRF token invalid or missing",
                        path);
                return;
            }
            response.sendRedirect("/login?expired");
            return;
        }
        if (isAnonymousOnlyPath(path)) {
            response.sendRedirect("/me");
            return;
        }
        if (apiRequest) {
            writeJsonError(response,
                    HttpStatus.FORBIDDEN,
                    "Access denied",
                    path);
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private boolean isAnonymousOnlyPath(String path) {
        if ("/login".equals(path) || "/register".equals(path) || "/restore".equals(path)) {
            return true;
        }
        return path != null && (path.startsWith("/register/") || path.startsWith("/restore/"));
    }

    private boolean isApiPath(String path) {
        return path != null && path.startsWith("/api/");
    }

    private String resolvePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

    private void writeJsonError(HttpServletResponse response, HttpStatus status, String message, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiErrorResponse apiError = ApiErrorResponse.of(status, message, path);
        objectMapper.writeValue(response.getWriter(), apiError);
    }
}
