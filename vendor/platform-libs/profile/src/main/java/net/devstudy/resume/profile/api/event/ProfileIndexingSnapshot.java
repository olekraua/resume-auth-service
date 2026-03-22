package net.devstudy.resume.profile.api.event;

import java.time.LocalDate;
import java.util.List;

public record ProfileIndexingSnapshot(Long profileId, String uid, String firstName, String lastName,
        String city, String country, String smallPhoto, LocalDate birthDay,
        String objective, String summary, String info, List<String> skills) {

    public ProfileIndexingSnapshot {
        skills = skills == null ? List.of() : List.copyOf(skills);
    }
}
