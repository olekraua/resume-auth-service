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

/**
 * @author devstudy
 * @see http://devstudy.net
 */
@Entity
@Table(name = "course")
public class Course extends AbstractFinishDateEntity<Long> implements ProfileEntity {
    @Serial
    private static final long serialVersionUID = 4206575925684228495L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @Size(max = 60)
    @Column(length = 60)
    private String name;

    @Size(max = 60)
    @Column(length = 60)
    private String school;

    // bi-directional many-to-one association to Profile
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profile", nullable = false)
    @JsonIgnore
    private Profile profile;

    @Override
    public Long getId() {
        return id;
    }

    // --- getters/setters ---

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public Profile getProfile() {
        return profile;
    }

    @Override
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    // --- equals/hashCode (1:1 Semantik beibehalten) ---

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                getFinishDate(),
                id,
                name,
                school
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (!(obj instanceof Course other)) return false;

        return Objects.equals(getFinishDate(), other.getFinishDate())
                && Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(school, other.school);
    }
}

