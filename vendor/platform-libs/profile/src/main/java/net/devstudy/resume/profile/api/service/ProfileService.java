package net.devstudy.resume.profile.api.service;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.devstudy.resume.profile.api.model.Certificate;
import net.devstudy.resume.profile.api.model.Course;
import net.devstudy.resume.profile.api.model.Education;
import net.devstudy.resume.profile.api.model.Language;
import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.api.model.Practic;
import net.devstudy.resume.profile.api.model.Skill;
import net.devstudy.resume.profile.api.dto.ContactsForm;
import net.devstudy.resume.profile.api.dto.InfoForm;

public interface ProfileService {
    Optional<Profile> findByUid(String uid);

    Optional<Profile> findWithAllByUid(String uid);

    Optional<Profile> findByIdWithAll(Long id);

    Page<Profile> findAll(Pageable pageable);

    Iterable<Profile> findAllForIndexing();

    Page<Profile> search(String query, Pageable pageable);

    void removeProfile(Long profileId);

    Profile register(String uid, String firstName, String lastName, String rawPassword);

    Optional<Profile> findById(Long id);

    void updateUid(Long profileId, String newUid);

    void updateSkills(Long profileId, List<Skill> items);

    void updatePractics(Long profileId, List<Practic> items);

    void updateEducations(Long profileId, List<Education> items);

    void updateCourses(Long profileId, List<Course> items);

    void updateLanguages(Long profileId, List<Language> items);

    void updateHobbies(Long profileId, List<Long> hobbyIds);

    void updateContacts(Long profileId, ContactsForm form);

    void updateInfo(Long profileId, InfoForm form);

    void updateCertificates(Long profileId, List<Certificate> items);

    void updateConnectionsVisibility(Long profileId, boolean visibleToConnections);

    void updatePhoto(Long profileId, String largeUrl, String smallUrl);

    void removePhoto(Long profileId);
}
