package net.devstudy.resume.web.controller;

import static net.devstudy.resume.shared.constants.Constants.UI.MAX_PROFILES_PER_PAGE;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import net.devstudy.resume.auth.api.model.CurrentProfile;
import net.devstudy.resume.auth.api.security.CurrentProfileProvider;
import net.devstudy.resume.profile.api.model.Certificate;
import net.devstudy.resume.profile.api.model.Contacts;
import net.devstudy.resume.profile.api.model.Course;
import net.devstudy.resume.profile.api.model.Education;
import net.devstudy.resume.profile.api.model.Language;
import net.devstudy.resume.profile.api.model.Practic;
import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.api.model.ProfileConnection;
import net.devstudy.resume.profile.api.model.ProfileConnectionState;
import net.devstudy.resume.profile.api.model.Skill;
import net.devstudy.resume.profile.api.service.ProfileConnectionService;
import net.devstudy.resume.profile.api.service.ProfileReadService;
import net.devstudy.resume.profile.api.service.ProfileService;
import net.devstudy.resume.shared.model.LanguageLevel;
import net.devstudy.resume.shared.model.LanguageType;
import net.devstudy.resume.staticdata.api.model.Hobby;
import net.devstudy.resume.staticdata.api.service.StaticDataService;
import net.devstudy.resume.web.dto.PageResponse;
import net.devstudy.resume.web.dto.ProfileSummary;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profiles")
public class ProfileApiController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ProfileService profileService;
    private final CurrentProfileProvider currentProfileProvider;
    private final ProfileConnectionService profileConnectionService;
    private final ProfileReadService profileReadService;
    private final StaticDataService staticDataService;

    @GetMapping
    public PageResponse<ProfileSummary> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", required = false) Integer size) {
        int safePage = Math.max(0, page);
        int safeSize = normalizeSize(size);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by("id"));
        Page<Profile> result = profileService.findAll(pageRequest);
        List<ProfileSummary> items = mapList(result.getContent(), ProfileSummary::from);
        return new PageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @GetMapping("/{uid}")
    public ProfileDetails profile(@PathVariable String uid) {
        Profile profile = profileService.findWithAllByUid(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        CurrentProfile currentProfile = currentProfileProvider.getCurrentProfile();
        boolean ownProfile = currentProfile != null && currentProfile.getId().equals(profile.getId());
        return toDetails(profile, ownProfile, currentProfile);
    }

    @GetMapping("/{uid}/connections")
    public PageResponse<ConnectionSummary> connections(@PathVariable String uid,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "company", required = false) String company,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "country", required = false) String country) {
        Profile profile = profileService.findByUid(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        CurrentProfile currentProfile = currentProfileProvider.getCurrentProfile();
        boolean ownProfile = currentProfile != null && currentProfile.getId().equals(profile.getId());
        ProfileConnectionState connectionState = resolveConnectionState(profile, ownProfile, currentProfile);
        if (!canViewConnections(profile, ownProfile, connectionState)) {
            return emptyConnectionPage(page, size);
        }
        List<ProfileConnection> connections = profileConnectionService.listConnections(profile.getId());
        List<ConnectionProfile> items = buildConnectionProfiles(connections, profile.getId(), null);
        return toConnectionPage(items, page, size, sort, order,
                new ConnectionFilter(
                        normalizeFilter(query),
                        normalizeFilter(name),
                        normalizeFilter(company),
                        normalizeFilter(city),
                        normalizeFilter(country)
                ));
    }

    @GetMapping("/{uid}/connections/mutual")
    public PageResponse<ConnectionSummary> mutualConnections(@PathVariable String uid,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "company", required = false) String company,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "country", required = false) String country) {
        Profile profile = profileService.findByUid(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        CurrentProfile currentProfile = currentProfileProvider.getCurrentProfile();
        boolean ownProfile = currentProfile != null && currentProfile.getId().equals(profile.getId());
        if (ownProfile || currentProfile == null) {
            return emptyConnectionPage(page, size);
        }
        List<ConnectionProfile> items = buildMutualConnectionProfiles(profile, currentProfile);
        return toConnectionPage(items, page, size, sort, order,
                new ConnectionFilter(
                        normalizeFilter(query),
                        normalizeFilter(name),
                        normalizeFilter(company),
                        normalizeFilter(city),
                        normalizeFilter(country)
                ));
    }

    private ProfileDetails toDetails(Profile profile, boolean ownProfile, CurrentProfile currentProfile) {
        LocalDate birthDay = profile.getBirthDay() == null ? null : profile.getBirthDay().toLocalDate();
        ContactsItem contacts = toContacts(profile.getContacts());
        List<SkillItem> skills = mapList(profile.getSkills(), this::toSkill);
        List<LanguageItem> languages = mapList(profile.getLanguages(), this::toLanguage);
        List<HobbyItem> hobbies = mapList(resolveHobbies(profile), this::toHobby);
        List<PracticItem> practics = mapList(profile.getPractics(), this::toPractic);
        List<CertificateItem> certificates = mapList(profile.getCertificates(), this::toCertificate);
        List<CourseItem> courses = mapList(profile.getCourses(), this::toCourse);
        List<EducationItem> educations = mapList(profile.getEducations(), this::toEducation);
        ProfileConnectionState connectionState = resolveConnectionState(profile, ownProfile, currentProfile);
        List<ProfileSummary> connections = resolveConnections(profile, ownProfile, connectionState);
        List<ProfileSummary> mutualConnections = resolveMutualConnections(profile, ownProfile, currentProfile);
        int connectionsCount = resolveConnectionsCount(profile);
        int mutualConnectionsCount = mutualConnections == null ? 0 : mutualConnections.size();
        return new ProfileDetails(
                profile.getUid(),
                profile.getFirstName(),
                profile.getLastName(),
                trimToEmpty(profile.getFullName()),
                profile.getAge(),
                birthDay,
                profile.getCity(),
                profile.getCountry(),
                profile.getObjective(),
                profile.getSummary(),
                profile.getInfo(),
                profile.getLargePhoto(),
                profile.getSmallPhoto(),
                profile.getPhone(),
                profile.getEmail(),
                profile.isCompleted(),
                ownProfile,
                connectionState,
                contacts,
                skills,
                languages,
                hobbies,
                practics,
                certificates,
                courses,
                educations,
                connections,
                mutualConnections,
                connectionsCount,
                mutualConnectionsCount
        );
    }

    private List<Hobby> resolveHobbies(Profile profile) {
        List<Long> hobbyIds = profile.getHobbyIds();
        if (hobbyIds == null || hobbyIds.isEmpty()) {
            return List.of();
        }
        List<Hobby> hobbies = staticDataService.findHobbiesByIds(hobbyIds);
        if (hobbies == null || hobbies.isEmpty()) {
            return List.of();
        }
        return hobbies.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Hobby::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private SkillItem toSkill(Skill skill) {
        return new SkillItem(skill.getCategory(), skill.getValue());
    }

    private LanguageItem toLanguage(Language language) {
        LanguageType type = language.getType();
        boolean hasType = type != null && type != LanguageType.ALL;
        return new LanguageItem(language.getName(), language.getLevel(), type, hasType);
    }

    private HobbyItem toHobby(Hobby hobby) {
        return new HobbyItem(hobby.getId(), hobby.getName(), hobby.getCssClassName());
    }

    private PracticItem toPractic(Practic practic) {
        return new PracticItem(
                practic.getCompany(),
                practic.getPosition(),
                practic.getResponsibilities(),
                practic.getBeginDate(),
                practic.getFinishDate(),
                practic.isFinish(),
                practic.getDemo(),
                practic.getSrc()
        );
    }

    private CertificateItem toCertificate(Certificate certificate) {
        return new CertificateItem(
                certificate.getName(),
                certificate.getIssuer(),
                certificate.getSmallUrl(),
                certificate.getLargeUrl()
        );
    }

    private CourseItem toCourse(Course course) {
        return new CourseItem(
                course.getName(),
                course.getSchool(),
                course.getFinishDate(),
                course.isFinish()
        );
    }

    private EducationItem toEducation(Education education) {
        return new EducationItem(
                education.getFaculty(),
                education.getSummary(),
                education.getUniversity(),
                education.getBeginYear(),
                education.getFinishYear(),
                education.isFinish()
        );
    }

    private ContactsItem toContacts(Contacts contacts) {
        if (contacts == null) {
            return new ContactsItem(null, null, null, null);
        }
        return new ContactsItem(
                contacts.getFacebook(),
                contacts.getLinkedin(),
                contacts.getGithub(),
                contacts.getStackoverflow()
        );
    }

    private ProfileConnectionState resolveConnectionState(Profile profile, boolean ownProfile,
            CurrentProfile currentProfile) {
        if (profile == null) {
            return ProfileConnectionState.NONE;
        }
        if (ownProfile) {
            return ProfileConnectionState.SELF;
        }
        if (currentProfile == null) {
            return ProfileConnectionState.NONE;
        }
        return profileConnectionService.getConnectionState(currentProfile.getId(), profile.getId());
    }

    private List<ProfileSummary> resolveConnections(Profile profile, boolean ownProfile,
            ProfileConnectionState connectionState) {
        if (profile == null) {
            return List.of();
        }
        if (ownProfile) {
            return loadConnections(profile.getId());
        }
        if (!profile.isConnectionsVisibleToConnections()) {
            return List.of();
        }
        if (connectionState != ProfileConnectionState.CONNECTED) {
            return List.of();
        }
        return loadConnections(profile.getId());
    }

    private List<ProfileSummary> resolveMutualConnections(Profile profile, boolean ownProfile,
            CurrentProfile currentProfile) {
        if (profile == null || ownProfile || currentProfile == null) {
            return List.of();
        }
        return mapConnectionProfilesToSummaries(buildMutualConnectionProfiles(profile, currentProfile));
    }

    private List<ProfileSummary> loadConnections(Long profileId) {
        List<ProfileConnection> connections = profileConnectionService.listConnections(profileId);
        return mapConnectionProfilesToSummaries(buildConnectionProfiles(connections, profileId, null));
    }

    private List<ConnectionProfile> buildMutualConnectionProfiles(Profile profile, CurrentProfile currentProfile) {
        List<ProfileConnection> viewerConnections = profileConnectionService.listConnections(currentProfile.getId());
        if (viewerConnections == null || viewerConnections.isEmpty()) {
            return List.of();
        }
        Set<Long> targetIds = collectConnectionIds(profileConnectionService.listConnections(profile.getId()),
                profile.getId());
        if (targetIds.isEmpty()) {
            return List.of();
        }
        return buildConnectionProfiles(viewerConnections, currentProfile.getId(), targetIds);
    }

    private List<ConnectionProfile> buildConnectionProfiles(List<ProfileConnection> connections,
            Long profileId,
            Set<Long> allowedIds) {
        if (connections == null || connections.isEmpty()) {
            return List.of();
        }
        Set<Long> otherIds = connections.stream()
                .map(connection -> resolveOtherId(connection, profileId))
                .filter(Objects::nonNull)
                .filter(id -> allowedIds == null || allowedIds.contains(id))
                .collect(Collectors.toSet());
        if (otherIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Profile> profilesById = profileReadService.findAllById(new ArrayList<>(otherIds)).stream()
                .collect(Collectors.toMap(Profile::getId, Function.identity()));
        List<ConnectionProfile> items = new ArrayList<>(connections.size());
        for (ProfileConnection connection : connections) {
            Long otherId = resolveOtherId(connection, profileId);
            if (otherId == null || (allowedIds != null && !allowedIds.contains(otherId))) {
                continue;
            }
            Profile other = profilesById.get(otherId);
            if (other == null) {
                continue;
            }
            Instant connectedAt = resolveConnectedAt(connection);
            items.add(new ConnectionProfile(other, connectedAt));
        }
        return items;
    }

    private List<ProfileSummary> mapConnectionProfilesToSummaries(List<ConnectionProfile> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<ProfileSummary> summaries = new ArrayList<>(items.size());
        for (ConnectionProfile item : items) {
            if (item == null || item.profile == null) {
                continue;
            }
            ProfileSummary summary = ProfileSummary.from(item.profile);
            if (summary != null) {
                summaries.add(summary);
            }
        }
        return summaries;
    }

    private List<ConnectionSummary> mapConnectionProfilesToConnectionSummaries(List<ConnectionProfile> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<ConnectionSummary> summaries = new ArrayList<>(items.size());
        for (ConnectionProfile item : items) {
            if (item == null || item.profile == null) {
                continue;
            }
            ConnectionSummary summary = toConnectionSummary(item.profile, item.connectedAt);
            if (summary != null) {
                summaries.add(summary);
            }
        }
        return summaries;
    }

    private ConnectionSummary toConnectionSummary(Profile profile, Instant connectedAt) {
        ProfileSummary summary = ProfileSummary.from(profile);
        if (summary == null) {
            return null;
        }
        return new ConnectionSummary(
                summary.uid(),
                summary.fullName(),
                summary.age(),
                summary.city(),
                summary.country(),
                summary.objective(),
                summary.summary(),
                summary.smallPhoto(),
                connectedAt
        );
    }

    private PageResponse<ConnectionSummary> toConnectionPage(List<ConnectionProfile> items,
            int page,
            Integer size,
            String sort,
            String order,
            ConnectionFilter filter) {
        int safePage = Math.max(0, page);
        int safeSize = normalizeSize(size);
        if (items == null || items.isEmpty()) {
            return new PageResponse<>(List.of(), safePage, safeSize, 0, 0, false);
        }
        List<ConnectionProfile> filtered = applyConnectionFilters(items, filter);
        if (filtered.isEmpty()) {
            return new PageResponse<>(List.of(), safePage, safeSize, 0, 0, false);
        }
        sortConnections(filtered, sort, order);
        int total = filtered.size();
        int from = Math.min(safePage * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<ConnectionSummary> pageItems = mapConnectionProfilesToConnectionSummaries(filtered.subList(from, to));
        int totalPages = (int) Math.ceil(total / (double) safeSize);
        boolean hasNext = safePage + 1 < totalPages;
        return new PageResponse<>(pageItems, safePage, safeSize, total, totalPages, hasNext);
    }

    private PageResponse<ConnectionSummary> emptyConnectionPage(int page, Integer size) {
        int safePage = Math.max(0, page);
        int safeSize = normalizeSize(size);
        return new PageResponse<>(List.of(), safePage, safeSize, 0, 0, false);
    }

    private List<ConnectionProfile> applyConnectionFilters(List<ConnectionProfile> items, ConnectionFilter filter) {
        if (filter == null || filter.isEmpty()) {
            return items;
        }
        List<ConnectionProfile> filtered = new ArrayList<>(items.size());
        for (ConnectionProfile item : items) {
            if (item == null || item.profile == null) {
                continue;
            }
            if (matchesFilter(item.profile, filter)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private boolean matchesFilter(Profile profile, ConnectionFilter filter) {
        String name = nameKey(profile);
        String objective = normalizeFilter(profile.getObjective());
        String summary = normalizeFilter(profile.getSummary());
        String city = normalizeFilter(profile.getCity());
        String country = normalizeFilter(profile.getCountry());
        return matchesName(filter, name)
                && matchesCompany(filter, objective, summary)
                && matchesCity(filter, city)
                && matchesCountry(filter, country)
                && matchesQuery(filter, name, objective, summary, city, country);
    }

    private boolean matchesName(ConnectionFilter filter, String name) {
        return filter.name == null || contains(name, filter.name);
    }

    private boolean matchesCompany(ConnectionFilter filter, String objective, String summary) {
        if (filter.company == null) {
            return true;
        }
        return contains(objective, filter.company) || contains(summary, filter.company);
    }

    private boolean matchesCity(ConnectionFilter filter, String city) {
        return filter.city == null || contains(city, filter.city);
    }

    private boolean matchesCountry(ConnectionFilter filter, String country) {
        return filter.country == null || contains(country, filter.country);
    }

    private boolean matchesQuery(ConnectionFilter filter, String... fields) {
        if (filter.query == null) {
            return true;
        }
        return containsAny(filter.query, fields);
    }

    private boolean containsAny(String needle, String... fields) {
        if (needle == null || needle.isBlank()) {
            return true;
        }
        for (String field : fields) {
            if (contains(field, needle)) {
                return true;
            }
        }
        return false;
    }

    private void sortConnections(List<ConnectionProfile> items, String sort, String order) {
        ConnectionSort sortValue = parseSort(sort);
        SortOrder orderValue = parseOrder(order, sortValue);
        Comparator<ConnectionProfile> comparator = switch (sortValue) {
            case NAME -> Comparator.comparing(item -> nameKey(item.profile), Comparator.nullsLast(String::compareTo));
            case COMPANY -> Comparator.comparing(item -> companyKey(item.profile),
                    Comparator.nullsLast(String::compareTo));
            case LOCATION -> Comparator
                    .comparing((ConnectionProfile item) -> countryKey(item.profile),
                            Comparator.nullsLast(String::compareTo))
                    .thenComparing(item -> cityKey(item.profile), Comparator.nullsLast(String::compareTo));
            case RECENT -> Comparator.comparing((ConnectionProfile item) -> item.connectedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };
        comparator = comparator.thenComparing(item -> item.profile == null ? null : item.profile.getId(),
                Comparator.nullsLast(Long::compareTo));
        if (orderValue == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        items.sort(comparator);
    }

    private ConnectionSort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return ConnectionSort.RECENT;
        }
        try {
            return ConnectionSort.valueOf(sort.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ConnectionSort.RECENT;
        }
    }

    private SortOrder parseOrder(String order, ConnectionSort sort) {
        if (order == null || order.isBlank()) {
            return sort == ConnectionSort.RECENT ? SortOrder.DESC : SortOrder.ASC;
        }
        try {
            return SortOrder.valueOf(order.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return sort == ConnectionSort.RECENT ? SortOrder.DESC : SortOrder.ASC;
        }
    }

    private boolean canViewConnections(Profile profile, boolean ownProfile, ProfileConnectionState connectionState) {
        if (profile == null) {
            return false;
        }
        if (ownProfile) {
            return true;
        }
        if (!profile.isConnectionsVisibleToConnections()) {
            return false;
        }
        return connectionState == ProfileConnectionState.CONNECTED;
    }

    private String nameKey(Profile profile) {
        if (profile == null) {
            return null;
        }
        String first = normalizeFilter(profile.getFirstName());
        String last = normalizeFilter(profile.getLastName());
        if (first == null && last == null) {
            return null;
        }
        if (first == null) {
            return last;
        }
        if (last == null) {
            return first;
        }
        return (first + " " + last).trim();
    }

    private String companyKey(Profile profile) {
        if (profile == null) {
            return null;
        }
        String objective = normalizeFilter(profile.getObjective());
        if (objective != null && !objective.isEmpty()) {
            return objective;
        }
        return normalizeFilter(profile.getSummary());
    }

    private String countryKey(Profile profile) {
        return profile == null ? null : normalizeFilter(profile.getCountry());
    }

    private String cityKey(Profile profile) {
        return profile == null ? null : normalizeFilter(profile.getCity());
    }

    private boolean contains(String haystack, String needle) {
        if (needle == null) {
            return true;
        }
        if (haystack == null) {
            return false;
        }
        return haystack.contains(needle);
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private Long resolveOtherId(ProfileConnection connection, Long profileId) {
        if (connection == null || profileId == null) {
            return null;
        }
        Long requesterId = safeProfileId(connection.getRequester());
        Long addresseeId = safeProfileId(connection.getAddressee());
        if (profileId.equals(requesterId)) {
            return addresseeId;
        }
        if (profileId.equals(addresseeId)) {
            return requesterId;
        }
        return null;
    }

    private Long safeProfileId(Profile profile) {
        return profile == null ? null : profile.getId();
    }

    private Instant resolveConnectedAt(ProfileConnection connection) {
        if (connection == null) {
            return null;
        }
        Instant responded = connection.getResponded();
        return responded != null ? responded : connection.getCreated();
    }

    private Set<Long> collectConnectionIds(List<ProfileConnection> connections, Long profileId) {
        if (connections == null || connections.isEmpty()) {
            return Set.of();
        }
        return connections.stream()
                .map(connection -> resolveOtherId(connection, profileId))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private int normalizeSize(Integer size) {
        int effective = size == null ? MAX_PROFILES_PER_PAGE : size;
        return Math.max(1, Math.min(effective, MAX_PAGE_SIZE));
    }

    private int resolveConnectionsCount(Profile profile) {
        if (profile == null) {
            return 0;
        }
        List<ProfileConnection> connections = profileConnectionService.listConnections(profile.getId());
        return connections == null ? 0 : connections.size();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static <T, R> List<R> mapList(List<T> items, Function<T, R> mapper) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(mapper)
                .filter(Objects::nonNull)
                .toList();
    }

    public record ProfileDetails(
            String uid,
            String firstName,
            String lastName,
            String fullName,
            int age,
            LocalDate birthDay,
            String city,
            String country,
            String objective,
            String summary,
            String info,
            String largePhoto,
            String smallPhoto,
            String phone,
            String email,
            boolean completed,
            boolean ownProfile,
            ProfileConnectionState connectionStatus,
            ContactsItem contacts,
            List<SkillItem> skills,
            List<LanguageItem> languages,
            List<HobbyItem> hobbies,
            List<PracticItem> practics,
            List<CertificateItem> certificates,
            List<CourseItem> courses,
            List<EducationItem> educations,
            List<ProfileSummary> connections,
            List<ProfileSummary> mutualConnections,
            int connectionsCount,
            int mutualConnectionsCount
    ) {
    }

    public record ConnectionSummary(
            String uid,
            String fullName,
            int age,
            String city,
            String country,
            String objective,
            String summary,
            String smallPhoto,
            Instant connectedAt
    ) {
    }

    public record ContactsItem(
            String facebook,
            String linkedin,
            String github,
            String stackoverflow
    ) {
    }

    public record SkillItem(
            String category,
            String value
    ) {
    }

    public record LanguageItem(
            String name,
            LanguageLevel level,
            LanguageType type,
            boolean hasLanguageType
    ) {
    }

    public record HobbyItem(
            Long id,
            String name,
            String cssClassName
    ) {
    }

    public record PracticItem(
            String company,
            String position,
            String responsibilities,
            LocalDate beginDate,
            LocalDate finishDate,
            boolean finish,
            String demo,
            String src
    ) {
    }

    public record CertificateItem(
            String name,
            String issuer,
            String smallUrl,
            String largeUrl
    ) {
    }

    public record CourseItem(
            String name,
            String school,
            LocalDate finishDate,
            boolean finish
    ) {
    }

    public record EducationItem(
            String faculty,
            String summary,
            String university,
            Integer beginYear,
            Integer finishYear,
            boolean finish
    ) {
    }

    private record ConnectionProfile(Profile profile, Instant connectedAt) {
    }

    private record ConnectionFilter(String query, String name, String company, String city, String country) {
        boolean isEmpty() {
            return query == null && name == null && company == null && city == null && country == null;
        }
    }

    private enum ConnectionSort {
        RECENT,
        NAME,
        COMPANY,
        LOCATION
    }

    private enum SortOrder {
        ASC,
        DESC
    }
}
