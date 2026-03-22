package net.devstudy.resume.web.dto;

import net.devstudy.resume.profile.api.model.Profile;

public record ProfileSummary(
        String uid,
        String fullName,
        int age,
        String city,
        String country,
        String objective,
        String summary,
        String smallPhoto
) {
    public static ProfileSummary from(Profile profile) {
        if (profile == null) {
            return null;
        }
        String fullName = profile.getFullName();
        if (fullName == null) {
            fullName = "";
        } else {
            fullName = fullName.trim();
        }
        return new ProfileSummary(
                profile.getUid(),
                fullName,
                profile.getAge(),
                profile.getCity(),
                profile.getCountry(),
                profile.getObjective(),
                profile.getSummary(),
                profile.getSmallPhoto()
        );
    }
}
