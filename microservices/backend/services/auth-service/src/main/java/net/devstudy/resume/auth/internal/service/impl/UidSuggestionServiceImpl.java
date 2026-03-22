package net.devstudy.resume.auth.internal.service.impl;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.devstudy.resume.auth.internal.client.ProfileInternalClient;
import net.devstudy.resume.auth.api.service.UidSuggestionService;

@Service
public class UidSuggestionServiceImpl implements UidSuggestionService {

    private static final int MAX_UID_LENGTH = 50;
    private static final String UID_DELIMITER = "-";

    private final ProfileInternalClient profileInternalClient;
    private final int maxTries;
    private final String alphabet;
    private final int suffixLength;
    private final SecureRandom random = new SecureRandom();

    public UidSuggestionServiceImpl(
            ProfileInternalClient profileInternalClient,
            @Value("${uid.max-tries:20}") int maxTries,
            @Value("${uid.suffix.alphabet:abcdefghijklmnopqrstuvwxyz0123456789}") String alphabet,
            @Value("${uid.suffix.length:2}") int suffixLength) {
        this.profileInternalClient = profileInternalClient;
        this.maxTries = maxTries;
        this.alphabet = alphabet == null ? "" : alphabet;
        this.suffixLength = suffixLength;
    }

    @Override
    public List<String> suggest(String baseUid) {
        if (baseUid == null) {
            return List.of();
        }
        String normalized = baseUid.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.isEmpty()) {
            return List.of();
        }
        if (suffixLength <= 0 || alphabet.isEmpty()) {
            return List.of();
        }
        if (normalized.length() + UID_DELIMITER.length() + suffixLength > MAX_UID_LENGTH) {
            return List.of();
        }

        Set<String> suggestions = new LinkedHashSet<>();
        int attempts = Math.max(0, maxTries);
        for (int i = 0; i < attempts && suggestions.size() < attempts; i++) {
            String candidate = normalized + UID_DELIMITER + randomSuffix();
            if (!profileInternalClient.uidExists(candidate)) {
                suggestions.add(candidate);
            }
        }
        return new ArrayList<>(suggestions);
    }

    private String randomSuffix() {
        StringBuilder builder = new StringBuilder(suffixLength);
        for (int i = 0; i < suffixLength; i++) {
            int idx = random.nextInt(alphabet.length());
            builder.append(alphabet.charAt(idx));
        }
        return builder.toString();
    }
}
