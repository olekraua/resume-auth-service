package net.devstudy.resume.profile.api.model;

import java.io.Serial;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;

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
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author devstudy
 * @see http://devstudy.net
 */
@Entity
@Table(name = "practic")
public class Practic extends AbstractFinishDateEntity<Long> implements ProfileEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank
    private String company;

    @Column(nullable = true, length = 255)
    private String demo;

    @Column(nullable = true, length = 255)
    private String src;

    @Column(name = "job_position", nullable = false, length = 100)
    @NotBlank
    private String position;

    @Column(nullable = false, length = 2147483647)
    @NotBlank
    private String responsibilities;

    @Column(name = "begin_date", nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull
    private LocalDate beginDate;

    @Transient
    private Integer beginDateMonth;

    @Transient
    private Integer beginDateYear;

    // bi-directional many-to-one association to Profile
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_profile", nullable = false)
    @JsonIgnore
    private Profile profile;

    public Practic() {
    }

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getCompany() {
        return company;
    }
    public void setCompany(String company) {
        this.company = company;
    }

    public String getDemo() {
        return demo;
    }
    public void setDemo(String demo) {
        this.demo = demo;
    }

    public String getSrc() {
        return src;
    }
    public void setSrc(String src) {
        this.src = src;
    }

    public String getPosition() {
        return position;
    }
    public void setPosition(String position) {
        this.position = position;
    }

    public String getResponsibilities() {
        return responsibilities;
    }
    public void setResponsibilities(String responsibilities) {
        this.responsibilities = responsibilities;
    }

    public Profile getProfile() {
        return profile;
    }

    @Override
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public LocalDate getBeginDate() {
        return beginDate;
    }
    public void setBeginDate(LocalDate beginDate) {
        this.beginDate = beginDate;
    }

    @Transient
    public Integer getBeginDateMonth() {
        return (beginDate != null) ? beginDate.getMonthValue() : null;
    }

    @Transient
    public Integer getBeginDateYear() {
        return (beginDate != null) ? beginDate.getYear() : null;
    }

    public void setBeginDateMonth(Integer beginDateMonth) {
        this.beginDateMonth = beginDateMonth;
        setupBeginDate();
    }

    public void setBeginDateYear(Integer beginDateYear) {
        this.beginDateYear = beginDateYear;
        setupBeginDate();
    }

    private void setupBeginDate() {
        if (beginDateYear != null && beginDateMonth != null) {
            this.beginDate = LocalDate.of(beginDateYear, beginDateMonth, 1);
        } else {
            this.beginDate = null;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            super.hashCode(),
            beginDate, company, demo, getFinishDate(),
            id, position, responsibilities, src
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (!(obj instanceof Practic other)) return false;
        return Objects.equals(beginDate, other.beginDate)
            && Objects.equals(company, other.company)
            && Objects.equals(demo, other.demo)
            && Objects.equals(getFinishDate(), other.getFinishDate())
            && Objects.equals(id, other.id)
            && Objects.equals(position, other.position)
            && Objects.equals(responsibilities, other.responsibilities)
            && Objects.equals(src, other.src);
    }
}

