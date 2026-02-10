package net.devstudy.resume.notification.internal.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import net.devstudy.resume.notification.internal.component.TemplateResolver;
import net.devstudy.resume.notification.internal.config.RestoreMailTemplateProperties;
import net.devstudy.resume.notification.internal.service.RestoreAccessMailService;

@Service
@ConditionalOnProperty(prefix = "app.restore.mail", name = "enabled", havingValue = "true")
public class RestoreAccessMailServiceImpl implements RestoreAccessMailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestoreAccessMailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final TemplateResolver templateResolver;
    private final String from;
    private final String subject;
    private final Duration tokenTtl;
    private final String textTemplate;
    private final String htmlTemplate;

    public RestoreAccessMailServiceImpl(JavaMailSender mailSender,
            TemplateResolver templateResolver,
            ResourceLoader resourceLoader,
            RestoreMailTemplateProperties templateProperties,
            @Value("${app.restore.mail.from:}") String from,
            @Value("${spring.mail.username:}") String username,
            @Value("${app.restore.mail.subject:Password reset}") String subject,
            @Value("${app.restore.token-ttl:PT1H}") Duration tokenTtl) {
        this.mailSender = mailSender;
        this.templateResolver = templateResolver;
        this.from = (from == null || from.isBlank()) ? username : from;
        this.subject = subject;
        this.tokenTtl = tokenTtl;
        this.textTemplate = loadTemplate(resourceLoader, templateProperties.getText());
        this.htmlTemplate = loadTemplate(resourceLoader, templateProperties.getHtml());
    }

    @Override
    public void sendRestoreLink(String email, String firstName, String link) {
        if (email == null || email.isBlank() || link == null || link.isBlank()) {
            return;
        }
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    "UTF-8");
            Map<String, Object> model = buildTemplateModel(firstName, link);
            String resolvedSubject;
            String resolvedText;
            String resolvedHtml;
            try {
                resolvedSubject = templateResolver.resolve(subject, model);
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("Failed to resolve restore access subject template: {}", ex.getMessage());
                return;
            }
            try {
                resolvedText = templateResolver.resolve(textTemplate, model);
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("Failed to resolve restore access text template: {}", ex.getMessage());
                return;
            }
            try {
                resolvedHtml = templateResolver.resolve(htmlTemplate, model);
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("Failed to resolve restore access html template: {}", ex.getMessage());
                return;
            }
            if (from != null && !from.isBlank()) {
                helper.setFrom(from);
            }
            helper.setTo(email);
            helper.setSubject(resolvedSubject);
            helper.setText(resolvedText, resolvedHtml);
            mailSender.send(message);
        } catch (MessagingException | MailException ex) {
            LOGGER.warn("Failed to send restore email to {}: {}", email, ex.getMessage());
        }
    }

    private String formatDuration(Duration duration) {
        if (duration == null) {
            return "a limited time";
        }
        long hours = duration.toHours();
        if (hours > 0) {
            return hours + " hour(s)";
        }
        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return minutes + " minute(s)";
        }
        long seconds = duration.toSeconds();
        if (seconds > 0) {
            return seconds + " second(s)";
        }
        return "a limited time";
    }

    private Map<String, Object> buildTemplateModel(String firstName, String link) {
        String safeFirstName = firstName == null ? "" : firstName.trim();
        return Map.of(
                "firstName", safeFirstName,
                "link", link,
                "tokenTtl", formatDuration(tokenTtl));
    }

    private String loadTemplate(ResourceLoader resourceLoader, String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalStateException("Restore access mail template location is blank");
        }
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("Restore access mail template not found: " + location);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            String template = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            if (template == null || template.isBlank()) {
                throw new IllegalStateException("Restore access mail template is empty: " + location);
            }
            return template;
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to load restore access mail template: " + location, ex);
        }
    }
}
