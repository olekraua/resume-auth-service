package net.devstudy.resume.profile.api.dto;

import java.sql.Date;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import net.devstudy.resume.shared.validation.annotation.Adulthood;

@Getter
@Setter
public class InfoForm {
    @Adulthood
    private Date birthDay;

    @Size(max = 100)
    private String country;

    @Size(max = 100)
    private String city;

    @NotBlank
    @Size(max = 2147483647)
    private String objective;

    @NotBlank
    @Size(max = 2147483647)
    private String summary;

    @Size(max = 2147483647)
    private String info;
}
