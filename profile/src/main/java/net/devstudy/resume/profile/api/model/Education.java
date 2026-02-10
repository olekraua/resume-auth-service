package net.devstudy.resume.profile.api.model;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.devstudy.resume.shared.model.AbstractEntity;

/**
 * @author devstudy
 * @see http://devstudy.net
 */
@Entity
@Table(name = "education")
public class Education extends AbstractEntity<Long> implements ProfileEntity {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(nullable = false, length = 255)
    @NotBlank
    private String faculty;

    @Column(nullable = false, length = 100)
    @NotBlank
    private String summary;

    @Column(nullable = false, length = 2147483647)
    @NotBlank
    private String university;

    @Column(name = "begin_year", nullable = false)
    @NotNull
    private Integer beginYear;

    @Column(name = "finish_year")
    private Integer finishYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profile", nullable = false)
    private Profile profile;

    public Education() {
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public Integer getBeginYear() {
        return beginYear;
    }

    public void setBeginYear(Integer beginYear) {
        this.beginYear = beginYear;
    }

    public Integer getFinishYear() {
        return finishYear;
    }

    public void setFinishYear(Integer finishYear) {
        this.finishYear = finishYear;
    }

    public Profile getProfile() {
        return profile;
    }

    @Override
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    @Transient
    public boolean isFinish() {
        return finishYear != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                id, faculty, summary, university, beginYear, finishYear
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (!(obj instanceof Education other)) return false;
        return Objects.equals(id, other.id)
                && Objects.equals(faculty, other.faculty)
                && Objects.equals(summary, other.summary)
                && Objects.equals(university, other.university)
                && Objects.equals(beginYear, other.beginYear)
                && Objects.equals(finishYear, other.finishYear);
    }
}
