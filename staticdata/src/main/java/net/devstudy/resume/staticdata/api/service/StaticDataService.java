package net.devstudy.resume.staticdata.api.service;

import java.util.List;
import java.util.Map;

import net.devstudy.resume.staticdata.api.model.Hobby;
import net.devstudy.resume.staticdata.api.model.SkillCategory;
import net.devstudy.resume.shared.model.LanguageLevel;
import net.devstudy.resume.shared.model.LanguageType;

public interface StaticDataService {
    List<Integer> findPracticsYears();

    List<Integer> findCoursesYears();

    List<Integer> findEducationYears();

    Map<Integer, String> findMonthMap();

    List<SkillCategory> findSkillCategories();

    List<LanguageType> findAllLanguageTypes();

    List<LanguageLevel> findAllLanguageLevels();

    List<Hobby> findAllHobbies();

    List<Hobby> findAllHobbiesWithSelected(List<Long> selectedIds);

    List<Hobby> findHobbiesByIds(List<Long> ids);
}
