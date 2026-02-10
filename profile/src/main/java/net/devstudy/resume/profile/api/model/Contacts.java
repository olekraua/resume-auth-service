package net.devstudy.resume.profile.api.model;

import java.io.Serial;
import java.io.Serializable;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Embeddable
@Access(AccessType.FIELD)
public class Contacts implements Serializable {
    @Serial
    private static final long serialVersionUID = -3685720846934765841L;

    @Size(max = 255)
    @URL
    @Column(length = 255)
    private String facebook;

    @Size(max = 255)
    @URL
    @Column(length = 255)
    private String linkedin;

    @Size(max = 255)
    @URL
    @Column(length = 255)
    private String github;

    @Size(max = 255)
    @URL
    @Column(length = 255)
    private String stackoverflow;

    public Contacts() { }

    public String getFacebook() {
        return facebook;
    }

    public void setFacebook(String facebook) {
        this.facebook = facebook;
    }

    public String getLinkedin() {
        return linkedin;
    }

    public void setLinkedin(String linkedin) {
        this.linkedin = linkedin;
    }

    public String getGithub() {
        return github;
    }

    public void setGithub(String github) {
        this.github = github;
    }

    public String getStackoverflow() {
        return stackoverflow;
    }

    public void setStackoverflow(String stackoverflow) {
        this.stackoverflow = stackoverflow;
    }
}

