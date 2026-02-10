package net.devstudy.resume.shared.component.impl;

import java.security.SecureRandom;
import java.util.Locale;

import org.springframework.stereotype.Component;

import net.devstudy.resume.shared.component.DataBuilder;
import net.devstudy.resume.shared.component.TranslitConverter;

@Component
public class DataBuilderImpl implements DataBuilder {

    private static final String UID_DELIMITER = "-";
    private static final String UID_ALLOWED_REGEX = "[^a-z0-9\\s_-]";
    private static final String UID_SEPARATORS_REGEX = "[\\s_-]+";
    private static final String CERT_ALLOWED_REGEX = "[^a-z0-9\\s_.-]";
    private static final String CERT_SEPARATORS_REGEX = "[\\s_.-]+";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final TranslitConverter translitConverter;

    public DataBuilderImpl(TranslitConverter translitConverter) {
        this.translitConverter = translitConverter;
    }

    @Override
    public String buildProfileUid(String firstName, String lastName) {
        String first = normalizeName(firstName);
        String last = normalizeName(lastName);
        String joined;
        if (first.isEmpty()) {
            joined = last;
        } else if (last.isEmpty()) {
            joined = first;
        } else {
            joined = first + UID_DELIMITER + last;
        }
        return truncate(joined, 64);
    }

    @Override
    public String buildRestoreAccessLink(String appHost, String token) {
        return appHost + "/restore/" + token;
    }

    @Override
    public String rebuildUidWithRandomSuffix(String baseUid, String alphabet, int letterCount) {
        return baseUid + UID_DELIMITER + generateRandomString(alphabet, letterCount);
    }

    @Override
    public String buildCertificateName(String fileName) {
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

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        String normalized = translitConverter.translit(name)
                .trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "";
        }
        normalized = normalized.replaceAll(UID_ALLOWED_REGEX, "");
        if (normalized.isEmpty()) {
            return "";
        }
        normalized = normalized.replaceAll(UID_SEPARATORS_REGEX, UID_DELIMITER);
        return trimDelimiters(normalized);
    }

    private String trimDelimiters(String value) {
        if (value.isEmpty()) {
            return "";
        }
        int start = 0;
        int end = value.length();
        char delimiter = UID_DELIMITER.charAt(0);
        while (start < end && value.charAt(start) == delimiter) {
            start++;
        }
        while (end > start && value.charAt(end - 1) == delimiter) {
            end--;
        }
        return value.substring(start, end);
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

    private String generateRandomString(String alphabet, int letterCount) {
        if (letterCount <= 0) {
            return "";
        }
        StringBuilder uid = new StringBuilder(letterCount);
        for (int i = 0; i < letterCount; i++) {
            int idx = RANDOM.nextInt(alphabet.length());
            uid.append(alphabet.charAt(idx));
        }
        return uid.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
