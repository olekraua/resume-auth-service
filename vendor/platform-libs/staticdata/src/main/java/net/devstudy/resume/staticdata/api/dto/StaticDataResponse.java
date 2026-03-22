package net.devstudy.resume.staticdata.api.dto;

import java.util.List;

public record StaticDataResponse(
        List<SkillCategoryItem> skillCategories,
        List<LanguageTypeItem> languageTypes,
        List<LanguageLevelItem> languageLevels,
        List<HobbyItem> hobbies,
        List<Integer> practicYears,
        List<Integer> courseYears,
        List<Integer> educationYears,
        List<MonthItem> months
) {
    public record SkillCategoryItem(
            Long id,
            String category
    ) {
    }

    public record LanguageTypeItem(
            String code,
            String label
    ) {
    }

    public record LanguageLevelItem(
            String code,
            int sliderValue
    ) {
    }

    public record HobbyItem(
            Long id,
            String name,
            String cssClassName
    ) {
    }

    public record MonthItem(
            int value,
            String label
    ) {
    }
}
