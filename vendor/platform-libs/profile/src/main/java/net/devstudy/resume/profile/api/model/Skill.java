package net.devstudy.resume.profile.api.model;

import java.io.Serial;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import net.devstudy.resume.shared.validation.annotation.EnglishLanguage;
import net.devstudy.resume.shared.model.AbstractEntity;

@Entity
@Table(name = "skill")
public class Skill extends AbstractEntity<Long> implements ProfileEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(nullable = false, length = 50)
    @EnglishLanguage
    @Size(min = 1)
    private String category;

    // Hinweis: Länge aus Original übernommen (entspricht effektiv TEXT in Postgres)
    @Column(nullable = false, length = 2147483647)
    @EnglishLanguage
    @Size(min = 1)
    private String value;

    // bi-directional many-to-one association to Profile
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profile", nullable = false)
    @JsonIgnore
    private Profile profile;

    public Skill() {
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Profile getProfile() {
        return this.profile;
    }

    @Override
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    @Override
    public int hashCode() {
        // Verhalten wie zuvor: id, category, value berücksichtigen
        return Objects.hash(super.hashCode(), id, category, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (!(obj instanceof Skill other)) return false;
        return Objects.equals(id, other.id)
            && Objects.equals(category, other.category)
            && Objects.equals(value, other.value);
    }
}

