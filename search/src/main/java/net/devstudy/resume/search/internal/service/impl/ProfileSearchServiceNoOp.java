package net.devstudy.resume.search.internal.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.api.service.ProfileSearchService;

@Service
@ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "false")
public class ProfileSearchServiceNoOp implements ProfileSearchService {

    @Override
    public Page<Profile> search(String query, Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @Override
    public void reindexAll() {
        // no-op
    }

    @Override
    public void indexProfiles(List<Profile> profiles) {
        // no-op
    }

    @Override
    public void removeProfile(Long profileId) {
        // no-op
    }
}
