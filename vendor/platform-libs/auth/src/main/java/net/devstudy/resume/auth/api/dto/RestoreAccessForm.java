package net.devstudy.resume.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import net.devstudy.resume.shared.validation.annotation.RestoreIdentifier;

@Getter
@Setter
public class RestoreAccessForm {

    @NotBlank
    @Size(max = 100)
    @RestoreIdentifier
    private String identifier;
}
