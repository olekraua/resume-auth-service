package net.devstudy.resume.profile.api.model;

import java.io.Serial;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import net.devstudy.resume.shared.model.LanguageLevel;
import net.devstudy.resume.shared.model.LanguageType;
import net.devstudy.resume.shared.model.AbstractEntity;

@Entity
@Table(name = "language", uniqueConstraints = {
        @UniqueConstraint(name = "language_profile_name_type_key", columnNames = {"id_profile", "name", "type"})
})
public class Language extends AbstractEntity<Long> implements ProfileEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(nullable = false, length = 18)
    @Convert(converter = LanguageLevel.PersistJPAConverter.class)
    private LanguageLevel level;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 7)
    @Convert(converter = LanguageType.PersistJPAConverter.class)
    private LanguageType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profile", nullable = false)
    @JsonIgnore
    private Profile profile;

    public Language() {
    }

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public LanguageLevel getLevel() {
        return level;
    }
    public void setLevel(LanguageLevel level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public LanguageType getType() {
        return type;
    }
    public void setType(LanguageType type) {
        this.type = type;
    }

    public Profile getProfile() {
        return profile;
    }

    @Override
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    @Transient
    public boolean isHasLanguageType() {
        return type != LanguageType.ALL;
    }

    @Override
    public String toString() {
        return String.format("Language[id=%s, level=%s, name=%s, type=%s]", id, level, name, type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Language other)) return false;
        // Entities are equal if same class and same non-null id
        return Objects.equals(this.id, other.id);
    }
}
