package net.devstudy.resume.profile.api.model;

import java.io.Serial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import net.devstudy.resume.shared.model.AbstractEntity;

/**
 * @author devstudy
 * @see http://devstudy.net
 */
@Entity
@Table(name = "certificate")
public class Certificate extends AbstractEntity<Long> implements ProfileEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "large_url", nullable = false, length = 255)
    private String largeUrl;

    @NotBlank
    @Size(max = 255)
    @Column(name = "small_url", nullable = false, length = 255)
    private String smallUrl;

    @Size(min = 1, max = 255)
    @Pattern(regexp = "^[\\p{L}0-9 .,'-]+$", message = "Certificate name contains invalid characters")
    @Column(nullable = false, length = 50)
    private String name;

    @Size(min = 1, max = 50)
    @Pattern(regexp = "^[\\p{L}0-9 .,'-]+$", message = "Issuer contains invalid characters")
    @Column(nullable = false, length = 50)
    private String issuer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_profile", nullable = false)
    private Profile profile;

    public Certificate() {
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLargeUrl() {
        return largeUrl;
    }

    public void setLargeUrl(String largeUrl) {
        this.largeUrl = largeUrl;
    }

    public String getSmallUrl() {
        return smallUrl;
    }

    public void setSmallUrl(String smallUrl) {
        this.smallUrl = smallUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Profile getProfile() {
        return profile;
    }

    @Override
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Certificate))
            return false;
        return super.equals(obj);
    }
}
