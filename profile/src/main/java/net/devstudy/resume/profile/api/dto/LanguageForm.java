package net.devstudy.resume.profile.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import net.devstudy.resume.profile.api.model.Language;

@Getter
@Setter
public class LanguageForm {
    @Valid
    private List<Language> items;
}
