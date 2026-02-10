package net.devstudy.resume.profile.api.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.devstudy.resume.profile.api.model.Profile;

public interface ProfileSearchService {
    Page<Profile> search(String query, Pageable pageable);

    void reindexAll();

    void indexProfiles(List<Profile> profiles);

    void removeProfile(Long profileId);
}
