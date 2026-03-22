package net.devstudy.resume.search.internal.mapper;

import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.search.internal.document.ProfileSearchDocument;

public interface ProfileSearchMapper {
    ProfileSearchDocument toDocument(Profile profile);
}
