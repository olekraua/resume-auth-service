package net.devstudy.resume.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import net.devstudy.resume.shared.validation.annotation.PasswordsMatch;

@Getter
@Setter
@PasswordsMatch
public class RegistrationForm {

    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "UID може містити лише літери, цифри, '-' або '_'")
    private String uid;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{L}][\\p{L}\\p{M}\\s\\-']{1,49}$", message = "Тільки літери, пробіл, апостроф або дефіс")
    private String firstName;

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{L}][\\p{L}\\p{M}\\s\\-']{1,49}$", message = "Тільки літери, пробіл, апостроф або дефіс")
    private String lastName;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    @Size(min = 6, max = 100)
    private String confirmPassword;
}
