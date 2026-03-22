package net.devstudy.resume.profile.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import net.devstudy.resume.profile.api.model.Course;

@Getter
@Setter
public class CourseForm {
    @Valid
    @NotEmpty
    private List<Course> items;
}
