package net.devstudy.resume.profile.api.dto;

import java.sql.Date;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileMainForm {

    private MultipartFile profilePhoto;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthDay;

    @Size(max = 100)
    private String country;

    @Size(max = 100)
    private String city;

    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String objective;

    @Size(max = 2147483647)
    private String summary;

    @Size(max = 2147483647)
    private String info;
}
