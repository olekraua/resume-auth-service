package net.devstudy.resume.profile.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactsForm {

    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 255)
    private String facebook;

    @Size(max = 255)
    private String linkedin;

    @Size(max = 255)
    private String github;

    @Size(max = 255)
    private String stackoverflow;
}
