package net.devstudy.resume.profile.internal.security;

import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

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

    private static final String CLIENT_CERT_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";

    @Value("${app.internal.api.mtls.enabled:true}")
    private boolean mtlsEnabled;

    @Value("${app.internal.api.mtls.required-subject:}")
    private String requiredSubject;

    @Value("${app.internal.api.mtls.required-san-dns:}")
    private String requiredSanDns;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!mtlsEnabled) {
            filterChain.doFilter(request, response);
            return;
        }
        X509Certificate certificate = resolveClientCertificate(request);
        if (certificate == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Client certificate required");
            return;
        }
        if (!isAllowedSubject(certificate)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Client certificate subject is not allowed");
            return;
        }
        if (!isAllowedSanDns(certificate)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Client certificate SAN is not allowed");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private X509Certificate resolveClientCertificate(HttpServletRequest request) {
        Object attribute = request.getAttribute(CLIENT_CERT_ATTRIBUTE);
        if (!(attribute instanceof X509Certificate[] certificates) || certificates.length == 0) {
            return null;
        }
        return certificates[0];
    }

    private boolean isAllowedSubject(X509Certificate certificate) {
        if (!StringUtils.hasText(requiredSubject)) {
            return true;
        }
        String subject = certificate.getSubjectX500Principal().getName();
        return subject != null && subject.contains(requiredSubject.trim());
    }

    private boolean isAllowedSanDns(X509Certificate certificate) {
        if (!StringUtils.hasText(requiredSanDns)) {
            return true;
        }
        try {
            Collection<List<?>> subjectAlternativeNames = certificate.getSubjectAlternativeNames();
            if (subjectAlternativeNames == null || subjectAlternativeNames.isEmpty()) {
                return false;
            }
            String expectedDns = requiredSanDns.trim();
            for (List<?> entry : subjectAlternativeNames) {
                if (entry == null || entry.size() < 2) {
                    continue;
                }
                Object type = entry.get(0);
                Object value = entry.get(1);
                if (type instanceof Integer sanType
                        && sanType == 2
                        && value instanceof String dnsName
                        && expectedDns.equalsIgnoreCase(dnsName)) {
                    return true;
                }
            }
            return false;
        } catch (CertificateParsingException ex) {
            return false;
        }
    }
}
