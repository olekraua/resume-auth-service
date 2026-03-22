package net.devstudy.resume.staticdata.internal.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.shared.model.LanguageLevel;
import net.devstudy.resume.shared.model.LanguageType;
import net.devstudy.resume.staticdata.api.dto.StaticDataResponse;
import net.devstudy.resume.staticdata.api.model.Hobby;
import net.devstudy.resume.staticdata.api.model.SkillCategory;
import net.devstudy.resume.staticdata.api.service.StaticDataService;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.services.staticdata.mode", havingValue = "remote")
public class HttpStaticDataService implements StaticDataService {

    private final RestClient staticDataRestClient;

    @Override
    public List<Integer> findPracticsYears() {
        return safeList(fetchStaticData().practicYears());
    }

    @Override
    public List<Integer> findCoursesYears() {
        return safeList(fetchStaticData().courseYears());
    }

    @Override
    public List<Integer> findEducationYears() {
        return safeList(fetchStaticData().educationYears());
    }

    @Override
    public Map<Integer, String> findMonthMap() {
        List<StaticDataResponse.MonthItem> items = fetchStaticData().months();
        Map<Integer, String> months = new HashMap<>();
        if (items == null || items.isEmpty()) {
            return months;
        }
        for (StaticDataResponse.MonthItem item : items) {
            if (item == null) {
                continue;
            }
            months.put(item.value(), item.label());
        }
        return months;
    }

    @Override
    public List<SkillCategory> findSkillCategories() {
        return mapSkillCategories(fetchStaticData().skillCategories());
    }

    @Override
    public List<LanguageType> findAllLanguageTypes() {
        List<StaticDataResponse.LanguageTypeItem> items = fetchStaticData().languageTypes();
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<LanguageType> result = new ArrayList<>(items.size());
        for (StaticDataResponse.LanguageTypeItem item : items) {
            if (item == null || item.code() == null) {
                continue;
            }
            try {
                result.add(LanguageType.valueOf(item.code()));
            } catch (IllegalArgumentException ignored) {
                // ignore unknown values from remote service
            }
        }
        return result;
    }

    @Override
    public List<LanguageLevel> findAllLanguageLevels() {
        List<StaticDataResponse.LanguageLevelItem> items = fetchStaticData().languageLevels();
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<LanguageLevel> result = new ArrayList<>(items.size());
        for (StaticDataResponse.LanguageLevelItem item : items) {
            if (item == null || item.code() == null) {
                continue;
            }
            try {
                result.add(LanguageLevel.valueOf(item.code()));
            } catch (IllegalArgumentException ignored) {
                // ignore unknown values from remote service
            }
        }
        return result;
    }

    @Override
    public List<Hobby> findAllHobbies() {
        return mapHobbies(fetchStaticData().hobbies());
    }

    @Override
    public List<Hobby> findAllHobbiesWithSelected(List<Long> selectedIds) {
        List<Hobby> all = mapHobbies(fetchStaticData().hobbies());
        if (all.isEmpty()) {
            return all;
        }
        Set<Long> selected = selectedIds == null ? Set.of()
                : new HashSet<>(selectedIds.stream().filter(Objects::nonNull).toList());
        if (selected.isEmpty()) {
            return all;
        }
        all.forEach(hobby -> hobby.setSelected(selected.contains(hobby.getId())));
        return all;
    }

    @Override
    public List<Hobby> findHobbiesByIds(List<Long> ids) {
        List<Long> safeIds = ids == null ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (safeIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Hobby> hobbiesById = mapHobbiesById(fetchStaticData().hobbies());
        if (hobbiesById.isEmpty()) {
            return List.of();
        }
        List<Hobby> result = new ArrayList<>(safeIds.size());
        for (Long id : safeIds) {
            Hobby hobby = hobbiesById.get(id);
            if (hobby != null) {
                result.add(hobby);
            }
        }
        return result;
    }

    private StaticDataResponse fetchStaticData() {
        StaticDataResponse response = staticDataRestClient.get()
                .uri("/api/static-data")
                .retrieve()
                .body(StaticDataResponse.class);
        if (response == null) {
            throw new IllegalStateException("Static data response is empty");
        }
        return response;
    }

    private List<Integer> safeList(List<Integer> values) {
        return values == null ? List.of() : values;
    }

    private List<SkillCategory> mapSkillCategories(List<StaticDataResponse.SkillCategoryItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<SkillCategory> result = new ArrayList<>(items.size());
        for (StaticDataResponse.SkillCategoryItem item : items) {
            if (item == null || item.id() == null || item.category() == null) {
                continue;
            }
            SkillCategory category = new SkillCategory();
            category.setId(item.id());
            category.setCategory(item.category());
            result.add(category);
        }
        return result;
    }

    private List<Hobby> mapHobbies(List<StaticDataResponse.HobbyItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<Hobby> result = new ArrayList<>(items.size());
        for (StaticDataResponse.HobbyItem item : items) {
            if (item == null || item.id() == null || item.name() == null) {
                continue;
            }
            Hobby hobby = new Hobby();
            hobby.setId(item.id());
            hobby.setName(item.name());
            result.add(hobby);
        }
        return result;
    }

    private Map<Long, Hobby> mapHobbiesById(List<StaticDataResponse.HobbyItem> items) {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        Map<Long, Hobby> result = new HashMap<>();
        for (StaticDataResponse.HobbyItem item : items) {
            if (item == null || item.id() == null || item.name() == null) {
                continue;
            }
            Hobby hobby = new Hobby();
            hobby.setId(item.id());
            hobby.setName(item.name());
            result.put(item.id(), hobby);
        }
        return result;
    }
}
