package net.devstudy.resume.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import net.devstudy.resume.shared.validation.annotation.FieldMatch;

@Getter
@Setter
@FieldMatch(first = "newPassword", second = "confirmPassword", message = "{password.match}")
public class PasswordForm {
    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;

    @NotBlank
    @Size(min = 6, max = 100)
    private String confirmPassword;
}
