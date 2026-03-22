package net.devstudy.resume.media.internal.component;

import java.util.Locale;

import org.springframework.stereotype.Component;

import net.devstudy.resume.shared.component.TranslitConverter;

@Component
public class CertificateNameBuilder {

    private static final String CERT_ALLOWED_REGEX = "[^a-z0-9\\s_.-]";
    private static final String CERT_SEPARATORS_REGEX = "[\\s_.-]+";
    private final TranslitConverter translitConverter;

    public CertificateNameBuilder(TranslitConverter translitConverter) {
        this.translitConverter = translitConverter;
    }

    public String build(String fileName) {
        String baseName = stripExtension(fileName);
        if (baseName.isEmpty()) {
            return "";
        }
        String normalized = translitConverter.translit(baseName)
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "";
        }
        normalized = normalized.replaceAll(CERT_ALLOWED_REGEX, "");
        if (normalized.isEmpty()) {
            return "";
        }
        normalized = normalized.replaceAll(CERT_SEPARATORS_REGEX, " ").trim();
        return capitalizeWords(normalized);
    }

    private String stripExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int point = fileName.lastIndexOf('.');
        if (point != -1) {
            return fileName.substring(0, point);
        }
        return fileName;
    }

    private String capitalizeWords(String value) {
        if (value.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(value.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isWhitespace(ch)) {
                capitalizeNext = true;
                result.append(ch);
                continue;
            }
            if (capitalizeNext) {
                result.append(Character.toUpperCase(ch));
                capitalizeNext = false;
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
