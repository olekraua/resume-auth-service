package net.devstudy.resume.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import net.devstudy.resume.shared.validation.annotation.PasswordsMatch;

@Getter
@Setter
@PasswordsMatch
public class RestorePasswordForm {

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    @Size(min = 6, max = 100)
    private String confirmPassword;
}
