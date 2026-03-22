package net.devstudy.resume.search.internal.service.impl;

import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.api.model.Skill;
import net.devstudy.resume.search.internal.document.ProfileSearchDocument;
import net.devstudy.resume.search.internal.mapper.ProfileSearchMapper;

@Component
public class ProfileSearchMapperImpl implements ProfileSearchMapper {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PHONE_CANDIDATE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:\\+?\\d[\\d\\s().-]{7,}\\d)(?!\\d)");
    private static final int MIN_PHONE_DIGITS_TO_REDACT = 9;

    static final int MAX_NAME_LENGTH = 64;
    static final int MAX_FULL_NAME_LENGTH = 130;
    static final int MAX_OBJECTIVE_LENGTH = 5000;
    static final int MAX_SUMMARY_LENGTH = 5000;
    static final int MAX_INFO_LENGTH = 5000;
    static final int MAX_SKILL_VALUE_LENGTH = 128;
    static final int MAX_SKILLS_LENGTH = 2000;

    private final boolean indexInfo;
    private final boolean redactInfo;

    public ProfileSearchMapperImpl(
            @Value("${app.search.elasticsearch.privacy.index-info:true}") boolean indexInfo,
            @Value("${app.search.elasticsearch.privacy.redact-info:true}") boolean redactInfo) {
        this.indexInfo = indexInfo;
        this.redactInfo = redactInfo;
    }

    @Override
    public ProfileSearchDocument toDocument(Profile profile) {
        String first = normalizeText(profile.getFirstName(), MAX_NAME_LENGTH);
        String last = normalizeText(profile.getLastName(), MAX_NAME_LENGTH);
        String fullName = truncate((first + " " + last).trim(), MAX_FULL_NAME_LENGTH);
        String skills = extractSkills(profile.getSkills());
        String info = indexInfo ? normalizeText(profile.getInfo()) : "";
        if (redactInfo) {
            info = redactPersonalData(info);
        }
        info = truncate(info, MAX_INFO_LENGTH);
        return new ProfileSearchDocument(profile.getId(), profile.getUid(), first, last, fullName,
                normalizeText(profile.getCity(), 100),
                normalizeText(profile.getCountry(), 60),
                normalizeText(profile.getSmallPhoto(), 255),
                profile.getBirthDay() != null ? profile.getBirthDay().toLocalDate() : null,
                normalizeText(profile.getObjective(), MAX_OBJECTIVE_LENGTH),
                normalizeText(profile.getSummary(), MAX_SUMMARY_LENGTH),
                info,
                skills);
    }

    private String extractSkills(java.util.List<Skill> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }
        String joined = skills.stream()
                .map(Skill::getValue)
                .map(value -> normalizeText(value, MAX_SKILL_VALUE_LENGTH))
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
        return truncate(joined, MAX_SKILLS_LENGTH);
    }

    private String normalizeText(String value, int maxLength) {
        return truncate(normalizeText(value), maxLength);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String text = Jsoup.parse(value).text();
        return text.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String redactPersonalData(String value) {
        String withoutEmails = EMAIL_PATTERN.matcher(value).replaceAll("");
        String withoutPhones = redactPhoneNumbers(withoutEmails);
        return withoutPhones.replaceAll("\\s+", " ").trim();
    }

    private String redactPhoneNumbers(String value) {
        Matcher matcher = PHONE_CANDIDATE_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String candidate = matcher.group();
            int digits = countDigits(candidate);
            if (digits >= MIN_PHONE_DIGITS_TO_REDACT) {
                matcher.appendReplacement(sb, "");
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(candidate));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private int countDigits(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints <= maxLength) {
            return value;
        }
        int endIndex = value.offsetByCodePoints(0, maxLength);
        return value.substring(0, endIndex);
    }
}
