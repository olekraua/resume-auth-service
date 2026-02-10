package net.devstudy.resume.auth.internal.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import net.devstudy.resume.auth.internal.entity.RememberMeToken;
import net.devstudy.resume.auth.internal.repository.storage.RememberMeTokenRepository;
import net.devstudy.resume.auth.api.service.ProfileAccountService;
import net.devstudy.resume.profile.api.dto.internal.ProfileAuthResponse;

@Service
public class RememberMeService implements PersistentTokenRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RememberMeService.class);
    private static final Duration DEFAULT_TTL = Duration.ofDays(14);

    private final RememberMeTokenRepository rememberMeTokenRepository;
    private final ProfileAccountService profileAccountService;
    private final Duration tokenTtl;

    public RememberMeService(RememberMeTokenRepository rememberMeTokenRepository,
            ProfileAccountService profileAccountService,
            @Value("${app.security.remember-me.token-ttl:PT336H}") Duration tokenTtl) {
        this.rememberMeTokenRepository = rememberMeTokenRepository;
        this.profileAccountService = profileAccountService;
        this.tokenTtl = normalizeTtl(tokenTtl);
    }

    @Override
    @Transactional
    public void createNewToken(PersistentRememberMeToken token) {
        if (token == null || !hasText(token.getSeries()) || !hasText(token.getUsername())
                || !hasText(token.getTokenValue())) {
            return;
        }
        ProfileAuthResponse auth = profileAccountService.loadForAuth(token.getUsername().trim());
        if (auth == null || auth.id() == null) {
            LOGGER.debug("Remember-me token ignored: profile not found for uid={}", token.getUsername());
            return;
        }
        RememberMeToken entity = rememberMeTokenRepository.findBySeries(token.getSeries())
                .orElseGet(RememberMeToken::new);
        entity.setSeries(token.getSeries());
        entity.setToken(token.getTokenValue());
        entity.setLastUsed(toInstant(token.getDate()));
        entity.setProfileId(auth.id());
        entity.setUsername(auth.uid());
        rememberMeTokenRepository.save(entity);
    }

    @Override
    @Transactional
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        if (!hasText(series) || !hasText(tokenValue)) {
            return;
        }
        Optional<RememberMeToken> existing = rememberMeTokenRepository.findBySeries(series.trim());
        if (existing.isEmpty()) {
            LOGGER.debug("Remember-me update ignored: series not found={}", series);
            return;
        }
        RememberMeToken entity = existing.get();
        entity.setToken(tokenValue);
        entity.setLastUsed(toInstant(lastUsed));
        rememberMeTokenRepository.save(entity);
    }

    @Override
    @Transactional
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        if (!hasText(seriesId)) {
            return null;
        }
        Optional<RememberMeToken> token = rememberMeTokenRepository.findBySeries(seriesId.trim());
        if (token.isEmpty()) {
            return null;
        }
        RememberMeToken entity = token.get();
        if (!hasText(entity.getUsername())) {
            rememberMeTokenRepository.delete(entity);
            return null;
        }
        Instant lastUsed = entity.getLastUsed();
        if (isExpired(lastUsed)) {
            rememberMeTokenRepository.delete(entity);
            return null;
        }
        return new PersistentRememberMeToken(entity.getUsername(), entity.getSeries(), entity.getToken(),
                Date.from(lastUsed));
    }

    @Override
    @Transactional
    public void removeUserTokens(String username) {
        if (!hasText(username)) {
            return;
        }
        rememberMeTokenRepository.deleteByUsername(username.trim());
    }

    private Instant toInstant(Date lastUsed) {
        return lastUsed != null ? lastUsed.toInstant() : Instant.now();
    }

    private boolean isExpired(Instant lastUsed) {
        if (lastUsed == null) {
            return true;
        }
        return lastUsed.plus(tokenTtl).isBefore(Instant.now());
    }

    private Duration normalizeTtl(Duration value) {
        if (value == null || value.isNegative() || value.isZero()) {
            return DEFAULT_TTL;
        }
        return value;
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}
