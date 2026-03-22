package net.devstudy.resume.profile.api.model;

import java.io.Serial;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import net.devstudy.resume.profile.api.annotation.ProfileInfoField;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Embedded;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.devstudy.resume.shared.model.AbstractEntity;

/**
 * @author devstudy
 * @see http://devstudy.net
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "profile")
@ToString(exclude = {"certificates","educations","hobbyIds","languages","practics","skills","courses","contacts"})
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Profile extends AbstractEntity<Long> {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(name = "birth_day")
    private Date birthDay;

    @Column(length = 100)
    private String city;
    @Column(length = 60)
    private String country;

    @Column(name = "first_name", nullable = false, length = 64)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 64)
    private String lastName;

    @Column(length = 2147483647)
    private String objective;

    @Column(name = "large_photo", length = 255)
    @JsonIgnore
    private String largePhoto;

    @Column(name = "small_photo", length = 255)
    private String smallPhoto;

    @Column(length = 20, unique = true)
    @JsonIgnore
    private String phone;

    @Column(length = 100, unique = true)
    @JsonIgnore
    private String email;

    @ProfileInfoField
    @Column
    private String info;

    @Column(length = 2147483647)
    private String summary;

    @Column(nullable = false, length = 64, unique = true)
    private String uid;

    @Column(nullable = false)
    @JsonIgnore
    private boolean completed;

    @Column(name = "connections_visible", nullable = false)
    private boolean connectionsVisibleToConnections = true;

    @Column(name = "created", insertable = false)
    @JsonIgnore
    private Instant created;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certificate> certificates;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("finishYear DESC, beginYear DESC, id DESC")
    @JsonIgnore
    private List<Education> educations;

    @ElementCollection
    @CollectionTable(name = "profile_hobby", joinColumns = @JoinColumn(name = "id_profile"))
    @Column(name = "id_hobby")
    @JsonIgnore
    private List<Long> hobbyIds;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Language> languages;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("finishDate DESC")
    private List<Practic> practics;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Skill> skills;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("finishDate DESC")
    private List<Course> courses;

    @Embedded
    private Contacts contacts = new Contacts();

    @Override
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public Date getBirthDay() { return this.birthDay; }
    public void setBirthDay(Date birthDay) { this.birthDay = birthDay; }

    public String getCity() { return this.city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return this.country; }
    public void setCountry(String country) { this.country = country; }

    public String getFirstName() { return this.firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return this.lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getObjective() { return this.objective; }
    public void setObjective(String objective) { this.objective = objective; }

    public String getSummary() { return this.summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getUid() { return this.uid; }
    public void setUid(String uid) { this.uid = uid; }

    public List<Certificate> getCertificates() { return this.certificates; }
    public void setCertificates(List<Certificate> certificates) {
        this.certificates = certificates;
        updateListSetProfile(this.certificates);
    }

    public List<Education> getEducations() { return this.educations; }
    public void setEducations(List<Education> educations) {
        this.educations = educations;
        updateListSetProfile(this.educations);
    }

    public List<Long> getHobbyIds() { return this.hobbyIds; }
    public void setHobbyIds(List<Long> hobbyIds) {
        this.hobbyIds = hobbyIds;
    }

    public List<Language> getLanguages() { return this.languages; }
    public void setLanguages(List<Language> languages) {
        this.languages = languages;
        updateListSetProfile(this.languages);
    }

    public List<Practic> getPractics() { return this.practics; }
    public void setPractics(List<Practic> practics) {
        this.practics = practics;
        updateListSetProfile(this.practics);
    }

    public List<Skill> getSkills() { return this.skills; }
    public void setSkills(List<Skill> skills) {
        this.skills = skills;
        updateListSetProfile(this.skills);
    }

    public List<Course> getCourses() { return courses; }
    public void setCourses(List<Course> courses) {
        this.courses = courses;
        updateListSetProfile(this.courses);
    }

    public String getLargePhoto() { return largePhoto; }
    public void setLargePhoto(String largePhoto) { this.largePhoto = largePhoto; }

    public String getSmallPhoto() { return smallPhoto; }
    public void setSmallPhoto(String smallPhoto) { this.smallPhoto = smallPhoto; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public boolean isConnectionsVisibleToConnections() { return connectionsVisibleToConnections; }
    public void setConnectionsVisibleToConnections(boolean connectionsVisibleToConnections) {
        this.connectionsVisibleToConnections = connectionsVisibleToConnections;
    }

    public Instant getCreated() { return created; }
    public void setCreated(Instant created) { this.created = created; }

    @Transient
    public String getFullName() { return firstName + " " + lastName; }

    @Transient
    public int getAge() {
        if (birthDay == null) return 0;
        LocalDate birth = birthDay.toLocalDate();
        return Period.between(birth, LocalDate.now()).getYears();
    }

    @Transient
    public String getProfilePhoto() {
        return (largePhoto != null) ? largePhoto : "/static/img/profile-placeholder.png";
    }

    public String updateProfilePhotos(String largePhoto, String smallPhoto) {
        String oldLargeImage = this.largePhoto;
        setLargePhoto(largePhoto);
        setSmallPhoto(smallPhoto);
        return oldLargeImage;
    }

    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }

    // https://hibernate.atlassian.net/browse/HHH-7610
    public Contacts getContacts() {
        if (contacts == null) { contacts = new Contacts(); }
        return contacts;
    }
    public void setContacts(Contacts contacts) { this.contacts = contacts; }

    private void updateListSetProfile(List<? extends ProfileEntity> list) {
        if (list != null) {
            for (ProfileEntity entity : list) {
                entity.setProfile(this);
            }
        }
    }
}
