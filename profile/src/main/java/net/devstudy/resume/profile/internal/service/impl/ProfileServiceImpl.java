package net.devstudy.resume.profile.internal.service.impl;

import java.util.Locale;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.devstudy.resume.profile.api.model.Certificate;
import net.devstudy.resume.profile.api.model.Contacts;
import net.devstudy.resume.profile.api.model.Course;
import net.devstudy.resume.profile.api.model.Education;
import net.devstudy.resume.staticdata.api.model.Hobby;
import net.devstudy.resume.profile.api.model.Language;
import net.devstudy.resume.profile.api.model.Practic;
import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.api.model.Skill;
import net.devstudy.resume.profile.api.exception.UidAlreadyExistsException;
import net.devstudy.resume.profile.api.event.ProfileIndexingRequestedEvent;
import net.devstudy.resume.profile.api.event.ProfileIndexingSnapshot;
import net.devstudy.resume.profile.api.event.ProfileSearchRemovalRequestedEvent;
import net.devstudy.resume.shared.event.ProfileMediaCleanupRequestedEvent;
import net.devstudy.resume.profile.api.dto.ContactsForm;
import net.devstudy.resume.profile.api.dto.InfoForm;
import net.devstudy.resume.shared.model.LanguageType;
import net.devstudy.resume.profile.internal.repository.storage.CertificateRepository;
import net.devstudy.resume.profile.internal.repository.storage.CourseRepository;
import net.devstudy.resume.profile.internal.repository.storage.EducationRepository;
import net.devstudy.resume.staticdata.api.service.StaticDataService;
import net.devstudy.resume.profile.internal.repository.storage.LanguageRepository;
import net.devstudy.resume.profile.internal.repository.storage.PracticRepository;
import net.devstudy.resume.profile.internal.repository.storage.ProfileConnectionRepository;
import net.devstudy.resume.profile.internal.repository.storage.ProfileRepository;
import net.devstudy.resume.profile.internal.repository.storage.SkillRepository;
import net.devstudy.resume.profile.api.service.ProfileSearchService;
import net.devstudy.resume.profile.api.service.ProfileService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final PracticRepository practicRepository;
    private final EducationRepository educationRepository;
    private final CourseRepository courseRepository;
    private final LanguageRepository languageRepository;
    private final StaticDataService staticDataService;
    private final CertificateRepository certificateRepository;
    private final ProfileConnectionRepository profileConnectionRepository;
    private final ProfileSearchService profileSearchService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Optional<Profile> findByUid(String uid) {
        return profileRepository.findByUid(uid);
    }

    @Override
    public Optional<Profile> findWithAllByUid(String uid) {
        Optional<Profile> opt = profileRepository.findByUid(uid);
        opt.ifPresent(this::initializeCollections);
        return opt;
    }

    @Override
    public Optional<Profile> findByIdWithAll(Long id) {
        Optional<Profile> opt = profileRepository.findById(id);
        opt.ifPresent(this::initializeCollections);
        return opt;
    }

    @Override
    public Page<Profile> findAll(Pageable pageable) {
        return profileRepository.findAll(pageable);
    }

    @Override
    public Iterable<Profile> findAllForIndexing() {
        return profileRepository.findAll(Pageable.unpaged()).getContent();
    }

    @Override
    public Page<Profile> search(String query, Pageable pageable) {
        try {
            return profileSearchService.search(query, pageable);
        } catch (Exception ex) {
            // fallback на JPA, якщо ES недоступний
            return profileRepository.search(query, pageable);
        }
    }

    @Override
    @Transactional
    public Profile register(String uid, String firstName, String lastName, String rawPassword) {
        String normalizedUid = normalizeUid(uid);
        if (profileRepository.findByUid(normalizedUid).isPresent()) {
            throw new UidAlreadyExistsException(normalizedUid);
        }
        Profile profile = new Profile();
        profile.setUid(normalizedUid);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setCompleted(false);
        profile.setConnectionsVisibleToConnections(true);
        if (profile.getContacts() == null) {
            profile.setContacts(new Contacts());
        }
        Profile saved = profileRepository.save(profile);
        requestIndexing(saved, java.util.List.of());
        return saved;
    }

    @Override
    @Transactional
    public void removeProfile(Long profileId) {
        if (profileId == null) {
            return;
        }
        Profile profile = profileRepository.findById(profileId).orElse(null);
        if (profile == null) {
            return;
        }
        java.util.List<String> photoUrls = collectProfilePhotoUrls(profile);
        java.util.List<String> certificateUrls = collectProfileCertificateUrls(profileId);
        profileConnectionRepository.deleteByRequesterIdOrAddresseeId(profileId, profileId);
        profileRepository.delete(profile);
        publishMediaCleanup(photoUrls, certificateUrls, false);
        eventPublisher.publishEvent(new ProfileSearchRemovalRequestedEvent(profileId));
    }

    private boolean isProfileCompleted(Profile profile) {
        return profile.getBirthDay() != null
                && nonBlank(profile.getFirstName(), profile.getLastName(), profile.getUid(), profile.getEmail(),
                        profile.getPhone(), profile.getCountry(), profile.getCity(), profile.getObjective(),
                        profile.getSummary(), profile.getSmallPhoto());
    }

    private boolean nonBlank(String... values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String normalizeUid(String uid) {
        if (uid == null) {
            throw new IllegalArgumentException("Uid is required");
        }
        String candidate = uid.trim();
        if (!candidate.matches("^[A-Za-z0-9_-]+$")) {
            throw new IllegalArgumentException("Uid must contain only latin letters, digits, '-' or '_'");
        }
        String normalized = candidate.toLowerCase(Locale.ENGLISH);
        if (normalized.length() < 3 || normalized.length() > 64) {
            throw new IllegalArgumentException("Uid must be 3-64 chars (a-z, 0-9, '-', '_')");
        }
        return normalized;
    }

    @Override
    public Optional<Profile> findById(Long id) {
        return profileRepository.findById(id);
    }

    @Override
    @Transactional
    public void updateUid(Long profileId, String newUid) {
        Profile profile = getProfileOrThrow(profileId);
        String normalizedUid = normalizeUid(newUid);
        if (profileRepository.findByUid(normalizedUid).isPresent()) {
            throw new UidAlreadyExistsException(normalizedUid);
        }
        profile.setUid(normalizedUid);
        profileRepository.save(profile);
        requestIndexing(profile);
    }

    @Override
    @Transactional
    public void updateSkills(Long profileId, java.util.List<Skill> items) {
        Profile profile = getProfileOrThrow(profileId);
        skillRepository.deleteByProfileId(profileId);
        if (items != null) {
            for (Skill skill : items) {
                skill.setId(null);
                skill.setProfile(profile);
            }
            skillRepository.saveAll(items);
        }
        requestIndexing(profile, collectSkillValues(items));
    }

    @Override
    @Transactional
    public void updatePractics(Long profileId, java.util.List<Practic> items) {
        Profile profile = getProfileOrThrow(profileId);
        practicRepository.deleteByProfileId(profileId);
        if (items != null) {
            for (Practic item : items) {
                item.setId(null);
                item.setProfile(profile);
            }
            practicRepository.saveAll(items);
        }
        requestIndexing(profile);
    }

    @Override
    @Transactional
    public void updateEducations(Long profileId, java.util.List<Education> items) {
        Profile profile = getProfileOrThrow(profileId);
        educationRepository.deleteByProfileId(profileId);
        if (items != null) {
            for (Education item : items) {
                item.setId(null);
                item.setProfile(profile);
            }
            educationRepository.saveAll(items);
        }
        requestIndexing(profile);
    }

    @Override
    @Transactional
    public void updateCourses(Long profileId, java.util.List<Course> items) {
        Profile profile = getProfileOrThrow(profileId);
        courseRepository.deleteByProfileId(profileId);
        if (items != null) {
            for (Course item : items) {
                item.setId(null);
                item.setProfile(profile);
            }
            courseRepository.saveAll(items);
        }
        requestIndexing(profile);
    }

    @Override
    @Transactional
    public void updateLanguages(Long profileId, java.util.List<Language> items) {
        Profile profile = getProfileOrThrow(profileId);
        java.util.List<Language> existing = languageRepository.findByProfileId(profileId);
        java.util.Map<Long, Language> existingById = mapExistingLanguagesById(existing);
        java.util.List<Language> toSave = new java.util.ArrayList<>();
        java.util.Set<Long> incomingIds = new java.util.HashSet<>();

        addLanguagesToSave(items, existingById, toSave, incomingIds, profile);
        deleteRemovedLanguages(existing, incomingIds);
        saveLanguages(toSave);
        requestIndexing(profile);
    }

    @Override
    @Transactional
    public void updateHobbies(Long profileId, java.util.List<Long> hobbyIds) {
        Profile profile = getProfileOrThrow(profileId);
        java.util.List<Long> safeIds = hobbyIds == null ? java.util.List.of()
                : hobbyIds.stream()
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();
        java.util.List<Long> selectedIds = staticDataService.findHobbiesByIds(safeIds).stream()
                .map(Hobby::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        profile.setHobbyIds(new java.util.ArrayList<>(selectedIds));
        profile.setCompleted(isProfileCompleted(profile));
        profileRepository.save(profile);
        requestIndexing(profile);
    }

    @Override
    @Transactional
    public void updateContacts(Long profileId, ContactsForm form) {
        Profile profile = getProfileOrThrow(profileId);
        profile.setPhone(form.getPhone());
        profile.setEmail(form.getEmail());
        if (profile.getContacts() == null) {
            profile.setContacts(new Contacts());
        }
        profile.getContacts().setFacebook(form.getFacebook());
        profile.getContacts().setLinkedin(form.getLinkedin());
        profile.getContacts().setGithub(form.getGithub());
        profile.getContacts().setStackoverflow(form.getStackoverflow());
        profile.setCompleted(isProfileCompleted(profile));
        profileRepository.save(profile);
        requestIndexing(profile);
    }

    @Override
    @Transactional
    public void updateInfo(Long profileId, InfoForm form) {
        Profile profile = getProfileOrThrow(profileId);
        profile.setBirthDay(form.getBirthDay());
        profile.setCountry(form.getCountry());
        profile.setCity(form.getCity());
        profile.setObjective(form.getObjective());
        profile.setSummary(form.getSummary());
        profile.setInfo(form.getInfo());
        // оновлюємо completion-статус, якщо всі ключові поля заповнені
        profile.setCompleted(isProfileCompleted(profile));
        profileRepository.save(profile);
        requestIndexing(profile);
    }

    @Override
    @Transactional
    public void updateCertificates(Long profileId, java.util.List<Certificate> items) {
        java.util.List<String> oldUrls = collectCertificateUrls(certificateRepository.findByProfileId(profileId));
        java.util.Set<String> newUrls = collectCertificateUrlsAsSet(items);
        Profile profile = getProfileOrThrow(profileId);
        certificateRepository.deleteByProfileId(profileId);
        if (items != null) {
            for (Certificate item : items) {
                item.setId(null);
                item.setProfile(profile);
            }
            certificateRepository.saveAll(items);
        }
        publishCertificateCleanup(oldUrls, newUrls);
        profile.setCompleted(isProfileCompleted(profile));
        requestIndexing(profile);
    }

    @Override
    @Transactional
    public void updateConnectionsVisibility(Long profileId, boolean visibleToConnections) {
        Profile profile = getProfileOrThrow(profileId);
        profile.setConnectionsVisibleToConnections(visibleToConnections);
        profileRepository.save(profile);
    }

    @Override
    @Transactional
    public void updatePhoto(Long profileId, String largeUrl, String smallUrl) {
        Profile profile = getProfileOrThrow(profileId);
        java.util.List<String> oldUrls = collectProfilePhotoUrls(profile);
        profile.setLargePhoto(largeUrl);
        profile.setSmallPhoto(smallUrl);
        profile.setCompleted(isProfileCompleted(profile));
        profileRepository.save(profile);
        requestIndexing(profile);
        publishPhotoCleanup(oldUrls, collectPhotoUrls(largeUrl, smallUrl));
    }

    @Override
    @Transactional
    public void removePhoto(Long profileId) {
        Profile profile = getProfileOrThrow(profileId);
        java.util.List<String> oldUrls = collectProfilePhotoUrls(profile);
        profile.setLargePhoto(null);
        profile.setSmallPhoto(null);
        profile.setCompleted(isProfileCompleted(profile));
        profileRepository.save(profile);
        requestIndexing(profile);
        publishPhotoCleanup(oldUrls, java.util.Set.of());
    }

    private java.util.Map<Long, Language> mapExistingLanguagesById(java.util.List<Language> existing) {
        java.util.Map<Long, Language> existingById = new java.util.HashMap<>();
        if (existing == null) {
            return existingById;
        }
        for (Language item : existing) {
            if (item != null && item.getId() != null) {
                existingById.put(item.getId(), item);
            }
        }
        return existingById;
    }

    private void addLanguagesToSave(java.util.List<Language> items, java.util.Map<Long, Language> existingById,
            java.util.List<Language> toSave, java.util.Set<Long> incomingIds, Profile profile) {
        if (items == null) {
            return;
        }
        for (Language item : items) {
            if (item == null) {
                continue;
            }
            LanguageType resolvedType = resolveLanguageType(item);
            Language target = selectLanguageTarget(item, existingById, incomingIds, resolvedType);
            target.setProfile(profile);
            toSave.add(target);
        }
    }

    private LanguageType resolveLanguageType(Language item) {
        if (item.getType() == null) {
            return LanguageType.ALL;
        }
        return item.getType();
    }

    private Language selectLanguageTarget(Language item, java.util.Map<Long, Language> existingById,
            java.util.Set<Long> incomingIds, LanguageType resolvedType) {
        if (item.getId() == null) {
            item.setType(resolvedType);
            return item;
        }
        Language stored = existingById.get(item.getId());
        if (stored == null) {
            item.setId(null);
            item.setType(resolvedType);
            return item;
        }
        stored.setName(item.getName());
        stored.setLevel(item.getLevel());
        stored.setType(resolvedType);
        incomingIds.add(stored.getId());
        return stored;
    }

    private void deleteRemovedLanguages(java.util.List<Language> existing, java.util.Set<Long> incomingIds) {
        if (existing == null || existing.isEmpty()) {
            return;
        }
        java.util.List<Language> toDelete = new java.util.ArrayList<>();
        for (Language item : existing) {
            if (item == null || item.getId() == null) {
                continue;
            }
            if (!incomingIds.contains(item.getId())) {
                toDelete.add(item);
            }
        }
        if (!toDelete.isEmpty()) {
            languageRepository.deleteAll(toDelete);
        }
    }

    private void saveLanguages(java.util.List<Language> toSave) {
        if (!toSave.isEmpty()) {
            languageRepository.saveAll(toSave);
        }
    }

    private void requestIndexing(Profile profile) {
        if (profile == null) {
            return;
        }
        requestIndexing(profile, collectSkillValues(profile.getId()));
    }

    private void requestIndexing(Profile profile, java.util.List<String> skillValues) {
        if (profile == null || profile.getId() == null) {
            return;
        }
        ProfileIndexingSnapshot snapshot = new ProfileIndexingSnapshot(profile.getId(), profile.getUid(),
                profile.getFirstName(), profile.getLastName(),
                profile.getCity(), profile.getCountry(), profile.getSmallPhoto(),
                profile.getBirthDay() != null ? profile.getBirthDay().toLocalDate() : null,
                profile.getObjective(), profile.getSummary(), profile.getInfo(), copyNonNull(skillValues));
        eventPublisher.publishEvent(new ProfileIndexingRequestedEvent(snapshot));
    }

    private java.util.List<String> collectSkillValues(Long profileId) {
        if (profileId == null) {
            return java.util.List.of();
        }
        return collectSkillValues(skillRepository.findByProfileIdOrderByIdAsc(profileId));
    }

    private java.util.List<String> collectSkillValues(java.util.List<Skill> skills) {
        if (skills == null || skills.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<String> values = new java.util.ArrayList<>(skills.size());
        for (Skill skill : skills) {
            if (skill == null || skill.getValue() == null) {
                continue;
            }
            values.add(skill.getValue());
        }
        return values;
    }

    private java.util.List<String> collectProfilePhotoUrls(Profile profile) {
        java.util.List<String> urls = new java.util.ArrayList<>(2);
        if (profile == null) {
            return urls;
        }
        if (StringUtils.hasText(profile.getLargePhoto())) {
            urls.add(profile.getLargePhoto());
        }
        if (StringUtils.hasText(profile.getSmallPhoto())) {
            urls.add(profile.getSmallPhoto());
        }
        return urls;
    }

    private java.util.Set<String> collectPhotoUrls(String largeUrl, String smallUrl) {
        java.util.Set<String> urls = new java.util.HashSet<>(2);
        if (StringUtils.hasText(largeUrl)) {
            urls.add(largeUrl);
        }
        if (StringUtils.hasText(smallUrl)) {
            urls.add(smallUrl);
        }
        return urls;
    }

    private java.util.List<String> collectProfileCertificateUrls(Long profileId) {
        java.util.List<Certificate> certificates = certificateRepository.findByProfileId(profileId);
        if (certificates == null || certificates.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<String> urls = new java.util.ArrayList<>(certificates.size() * 2);
        for (Certificate certificate : certificates) {
            if (certificate == null) {
                continue;
            }
            if (StringUtils.hasText(certificate.getLargeUrl())) {
                urls.add(certificate.getLargeUrl());
            }
            if (StringUtils.hasText(certificate.getSmallUrl())) {
                urls.add(certificate.getSmallUrl());
            }
        }
        return urls;
    }

    private void publishMediaCleanup(java.util.Collection<String> photoUrls,
            java.util.Collection<String> certificateUrls, boolean clearCertificateTempLinks) {
        java.util.List<String> safePhotos = copyNonNull(photoUrls);
        java.util.List<String> safeCertificates = copyNonNull(certificateUrls);
        if (safePhotos.isEmpty() && safeCertificates.isEmpty() && !clearCertificateTempLinks) {
            return;
        }
        eventPublisher.publishEvent(new ProfileMediaCleanupRequestedEvent(safePhotos, safeCertificates,
                clearCertificateTempLinks));
    }

    private java.util.List<String> copyNonNull(java.util.Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<String> safeValues = new java.util.ArrayList<>(values.size());
        for (String value : values) {
            if (value != null) {
                safeValues.add(value);
            }
        }
        return safeValues;
    }

    private Profile getProfileOrThrow(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException("Profile not found: " + profileId));
    }

    private java.util.List<String> collectCertificateUrls(java.util.List<Certificate> certificates) {
        if (certificates == null || certificates.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<String> urls = new java.util.ArrayList<>();
        for (Certificate certificate : certificates) {
            if (certificate == null) {
                continue;
            }
            if (StringUtils.hasText(certificate.getLargeUrl())) {
                urls.add(certificate.getLargeUrl());
            }
            if (StringUtils.hasText(certificate.getSmallUrl())) {
                urls.add(certificate.getSmallUrl());
            }
        }
        return urls;
    }

    private java.util.Set<String> collectCertificateUrlsAsSet(java.util.List<Certificate> certificates) {
        if (certificates == null || certificates.isEmpty()) {
            return java.util.Set.of();
        }
        java.util.Set<String> urls = new java.util.HashSet<>();
        for (Certificate certificate : certificates) {
            if (certificate == null) {
                continue;
            }
            if (StringUtils.hasText(certificate.getLargeUrl())) {
                urls.add(certificate.getLargeUrl());
            }
            if (StringUtils.hasText(certificate.getSmallUrl())) {
                urls.add(certificate.getSmallUrl());
            }
        }
        return urls;
    }

    private void publishPhotoCleanup(java.util.List<String> oldUrls, java.util.Set<String> newUrls) {
        if (oldUrls == null || oldUrls.isEmpty()) {
            return;
        }
        java.util.Set<String> safeNewUrls = newUrls == null ? java.util.Set.of() : newUrls;
        java.util.List<String> toRemove = oldUrls.stream()
                .filter(url -> !safeNewUrls.contains(url))
                .toList();
        if (toRemove.isEmpty()) {
            return;
        }
        publishMediaCleanup(toRemove, java.util.List.of(), false);
    }

    private void publishCertificateCleanup(java.util.List<String> oldUrls, java.util.Set<String> newUrls) {
        java.util.List<String> safeOldUrls = oldUrls == null ? java.util.List.of() : oldUrls;
        java.util.Set<String> safeNewUrls = newUrls == null ? java.util.Set.of() : newUrls;
        java.util.List<String> toRemove = safeOldUrls.stream()
                .filter(url -> !safeNewUrls.contains(url))
                .toList();
        publishMediaCleanup(java.util.List.of(), toRemove, true);
    }

    private void initializeCollections(Profile profile) {
        // force lazy collections to load via separate queries to avoid
        // MultipleBagFetchException
        if (profile.getLanguages() != null) {
            profile.getLanguages().size();
        }
        if (profile.getHobbyIds() != null) {
            profile.getHobbyIds().size();
        }
        if (profile.getSkills() != null) {
            profile.getSkills().size();
        }
        if (profile.getPractics() != null) {
            profile.getPractics().size();
        }
        if (profile.getCertificates() != null) {
            profile.getCertificates().size();
        }
        if (profile.getCourses() != null) {
            profile.getCourses().size();
        }
        if (profile.getEducations() != null) {
            profile.getEducations().size();
        }
    }
}
