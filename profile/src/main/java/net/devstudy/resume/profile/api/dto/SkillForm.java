package net.devstudy.resume.profile.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import net.devstudy.resume.profile.api.model.Skill;

@Getter
@Setter
public class SkillForm {
    @Valid
    @NotEmpty
    private List<Skill> items;
}
