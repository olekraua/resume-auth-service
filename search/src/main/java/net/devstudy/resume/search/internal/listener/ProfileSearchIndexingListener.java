package net.devstudy.resume.search.internal.listener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.api.model.Skill;
import net.devstudy.resume.profile.api.event.ProfileIndexingRequestedEvent;
import net.devstudy.resume.profile.api.event.ProfileIndexingSnapshot;
import net.devstudy.resume.profile.api.event.ProfileSearchRemovalRequestedEvent;
import net.devstudy.resume.profile.api.service.ProfileSearchService;

@Component
@ConditionalOnProperty(name = "app.search.profile-db.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ProfileSearchIndexingListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileSearchIndexingListener.class);

    private final ProfileSearchService profileSearchService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProfileIndexingRequested(ProfileIndexingRequestedEvent event) {
        if (event == null || event.snapshot() == null || event.snapshot().profileId() == null) {
            return;
        }
        try {
            Profile profile = toProfile(event.snapshot());
            profileSearchService.indexProfiles(List.of(profile));
        } catch (Exception ex) {
            LOGGER.warn("Elasticsearch index update failed: {}", ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProfileSearchRemovalRequested(ProfileSearchRemovalRequestedEvent event) {
        if (event == null || event.profileId() == null) {
            return;
        }
        profileSearchService.removeProfile(event.profileId());
    }

    private Profile toProfile(ProfileIndexingSnapshot snapshot) {
        Profile profile = new Profile();
        profile.setId(snapshot.profileId());
        profile.setUid(snapshot.uid());
        profile.setFirstName(snapshot.firstName());
        profile.setLastName(snapshot.lastName());
        profile.setCity(snapshot.city());
        profile.setCountry(snapshot.country());
        profile.setSmallPhoto(snapshot.smallPhoto());
        if (snapshot.birthDay() != null) {
            profile.setBirthDay(java.sql.Date.valueOf(snapshot.birthDay()));
        }
        profile.setObjective(snapshot.objective());
        profile.setSummary(snapshot.summary());
        profile.setInfo(snapshot.info());
        profile.setSkills(toSkills(snapshot.skills()));
        return profile;
    }

    private java.util.List<Skill> toSkills(java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<Skill> skills = new java.util.ArrayList<>(values.size());
        for (String value : values) {
            Skill skill = new Skill();
            skill.setValue(value);
            skills.add(skill);
        }
        return skills;
    }
}
