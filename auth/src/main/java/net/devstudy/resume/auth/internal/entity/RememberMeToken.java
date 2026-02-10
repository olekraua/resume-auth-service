package net.devstudy.resume.auth.internal.entity;

import java.io.Serial;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import net.devstudy.resume.shared.model.AbstractEntity;

@Entity
@Table(name = "remember_me_token",
        indexes = {
                @Index(name = "idx_remember_me_profile", columnList = "profile_id"),
                @Index(name = "idx_remember_me_username", columnList = "username")
        })
public class RememberMeToken extends AbstractEntity<String> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(length = 64, nullable = false)
    private String series;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(name = "last_used", nullable = false)
    private Instant lastUsed;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    public RememberMeToken() {
    }

    public RememberMeToken(String series, String token, Instant lastUsed, Long profileId, String username) {
        this.series = series;
        this.token = token;
        this.lastUsed = lastUsed;
        this.profileId = profileId;
        this.username = username;
    }

    @Override
    public String getId() {
        return series;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Instant lastUsed) {
        this.lastUsed = lastUsed;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
