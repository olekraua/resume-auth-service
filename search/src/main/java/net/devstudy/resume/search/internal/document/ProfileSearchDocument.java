package net.devstudy.resume.search.internal.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;
import java.time.LocalDate;

/**
 * Elasticsearch документ для пошуку профілів.
 */
@Document(indexName = "profiles")
@Setting(settingPath = "/elasticsearch/profile-search-settings.json")
public class ProfileSearchDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String uid;

    @Field(type = FieldType.Text)
    private String firstName;

    @Field(type = FieldType.Text)
    private String lastName;

    @Field(type = FieldType.Text)
    private String fullName;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private String country;

    @Field(type = FieldType.Keyword)
    private String smallPhoto;

    @Field(type = FieldType.Date)
    private LocalDate birthDay;

    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english", searchAnalyzer = "english"),
                    @InnerField(suffix = "uk", type = FieldType.Text, analyzer = "ukrainian",
                            searchAnalyzer = "ukrainian")
            })
    private String objective;

    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english", searchAnalyzer = "english"),
                    @InnerField(suffix = "uk", type = FieldType.Text, analyzer = "ukrainian",
                            searchAnalyzer = "ukrainian")
            })
    private String summary;

    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english", searchAnalyzer = "english"),
                    @InnerField(suffix = "uk", type = FieldType.Text, analyzer = "ukrainian",
                            searchAnalyzer = "ukrainian")
            })
    private String info;

    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english", searchAnalyzer = "english"),
                    @InnerField(suffix = "uk", type = FieldType.Text, analyzer = "ukrainian",
                            searchAnalyzer = "ukrainian")
            })
    private String skills;

    public ProfileSearchDocument() {
    }

    public ProfileSearchDocument(Long id, String uid, String firstName, String lastName, String fullName,
            String city, String country, String smallPhoto, LocalDate birthDay,
            String objective, String summary, String info, String skills) {
        this.id = id;
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.city = city;
        this.country = country;
        this.smallPhoto = smallPhoto;
        this.birthDay = birthDay;
        this.objective = objective;
        this.summary = summary;
        this.info = info;
        this.skills = skills;
    }

    public Long getId() {
        return id;
    }

    public String getUid() {
        return uid;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getSmallPhoto() {
        return smallPhoto;
    }

    public LocalDate getBirthDay() {
        return birthDay;
    }

    public String getObjective() {
        return objective;
    }

    public String getSummary() {
        return summary;
    }

    public String getInfo() {
        return info;
    }

    public String getSkills() {
        return skills;
    }
}
