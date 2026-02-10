package net.devstudy.resume.profile.internal.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import net.devstudy.resume.profile.api.dto.CertificateForm;
import net.devstudy.resume.profile.api.dto.ContactsForm;
import net.devstudy.resume.profile.api.dto.CourseForm;
import net.devstudy.resume.profile.api.dto.EducationForm;
import net.devstudy.resume.profile.api.dto.HobbyForm;
import net.devstudy.resume.profile.api.dto.InfoForm;
import net.devstudy.resume.profile.api.dto.LanguageForm;
import net.devstudy.resume.profile.api.dto.PracticForm;
import net.devstudy.resume.profile.api.dto.ProfileMainForm;
import net.devstudy.resume.profile.api.dto.SkillForm;
import net.devstudy.resume.profile.api.model.Certificate;
import net.devstudy.resume.profile.api.model.Contacts;
import net.devstudy.resume.profile.api.model.Education;
import net.devstudy.resume.profile.api.model.Practic;
import net.devstudy.resume.profile.api.model.Profile;
import net.devstudy.resume.profile.api.service.EditProfileService;
import net.devstudy.resume.shared.model.LanguageType;

@Service
@RequiredArgsConstructor
public class EditProfileServiceImpl implements EditProfileService {

    private final Validator validator;
    private final MessageSource messageSource;

    @Override
    public ProfileMainForm toProfileMainForm(Profile profile) {
        ProfileMainForm form = new ProfileMainForm();
        if (profile == null) {
            return form;
        }
        form.setBirthDay(profile.getBirthDay());
        form.setCountry(profile.getCountry());
        form.setCity(profile.getCity());
        form.setEmail(profile.getEmail());
        form.setPhone(profile.getPhone());
        form.setObjective(profile.getObjective());
        form.setSummary(profile.getSummary());
        form.setInfo(profile.getInfo());
        return form;
    }

    @Override
    public InfoForm toInfoForm(ProfileMainForm form) {
        InfoForm infoForm = new InfoForm();
        if (form == null) {
            return infoForm;
        }
        infoForm.setBirthDay(form.getBirthDay());
        infoForm.setCountry(form.getCountry());
        infoForm.setCity(form.getCity());
        infoForm.setObjective(form.getObjective());
        infoForm.setSummary(form.getSummary());
        infoForm.setInfo(form.getInfo());
        return infoForm;
    }

    @Override
    public ContactsForm toContactsForm(ProfileMainForm form) {
        ContactsForm contactsForm = new ContactsForm();
        if (form == null) {
            return contactsForm;
        }
        contactsForm.setEmail(form.getEmail());
        contactsForm.setPhone(form.getPhone());
        return contactsForm;
    }

    @Override
    public Object formFromProfile(Object emptyForm, Profile profile) {
        if (emptyForm instanceof SkillForm skillForm) {
            skillForm.setItems(profile.getSkills());
            return skillForm;
        }
        if (emptyForm instanceof PracticForm practicForm) {
            practicForm.setItems(profile.getPractics());
            return practicForm;
        }
        if (emptyForm instanceof EducationForm educationForm) {
            educationForm.setItems(profile.getEducations());
            return educationForm;
        }
        if (emptyForm instanceof CourseForm courseForm) {
            courseForm.setItems(profile.getCourses());
            return courseForm;
        }
        if (emptyForm instanceof LanguageForm languageForm) {
            languageForm.setItems(profile.getLanguages());
            return languageForm;
        }
        if (emptyForm instanceof CertificateForm certificateForm) {
            certificateForm.setItems(profile.getCertificates());
            return certificateForm;
        }
        if (emptyForm instanceof HobbyForm hobbyForm) {
            List<Long> ids = profile.getHobbyIds() == null ? List.of() : profile.getHobbyIds();
            hobbyForm.setHobbyIds(ids);
            return hobbyForm;
        }
        if (emptyForm instanceof ContactsForm contactsForm) {
            if (profile.getContacts() == null) {
                profile.setContacts(new Contacts());
            }
            contactsForm.setPhone(profile.getPhone());
            contactsForm.setEmail(profile.getEmail());
            contactsForm.setFacebook(profile.getContacts().getFacebook());
            contactsForm.setLinkedin(profile.getContacts().getLinkedin());
            contactsForm.setGithub(profile.getContacts().getGithub());
            contactsForm.setStackoverflow(profile.getContacts().getStackoverflow());
            return contactsForm;
        }
        if (emptyForm instanceof InfoForm infoForm) {
            infoForm.setBirthDay(profile.getBirthDay());
            infoForm.setCountry(profile.getCountry());
            infoForm.setCity(profile.getCity());
            infoForm.setObjective(profile.getObjective());
            infoForm.setSummary(profile.getSummary());
            infoForm.setInfo(profile.getInfo());
            return infoForm;
        }
        return emptyForm;
    }

    @Override
    public List<Practic> preparePractics(PracticForm form, BindingResult bindingResult) {
        List<Practic> items = form == null || form.getItems() == null ? new ArrayList<>() : form.getItems();
        List<Practic> filtered = new ArrayList<>();
        for (Practic item : items) {
            if (!isPracticEmpty(item)) {
                filtered.add(item);
            }
        }
        if (form != null) {
            form.setItems(filtered);
        }
        if (filtered.isEmpty()) {
            bindingResult.reject("practics.empty", "Додайте хоча б одну практику");
        }
        if (!filtered.isEmpty()) {
            for (int i = 0; i < filtered.size(); i++) {
                Set<ConstraintViolation<Practic>> violations = validator.validate(filtered.get(i));
                for (ConstraintViolation<Practic> violation : violations) {
                    String fieldPath = "items[" + i + "]." + violation.getPropertyPath();
                    bindingResult.addError(new FieldError("form", fieldPath, violation.getMessage()));
                }
            }
        }
        return filtered;
    }

    @Override
    public List<Education> prepareEducations(EducationForm form, BindingResult bindingResult) {
        List<Education> items = form == null || form.getItems() == null ? new ArrayList<>() : form.getItems();
        List<Education> filtered = new ArrayList<>();
        for (Education item : items) {
            if (!isEducationEmpty(item)) {
                filtered.add(item);
            }
        }
        if (form != null) {
            form.setItems(filtered);
        }
        if (filtered.isEmpty()) {
            bindingResult.reject("education.empty", "Додайте хоча б одну освіту");
        }
        if (!filtered.isEmpty()) {
            for (int i = 0; i < filtered.size(); i++) {
                Set<ConstraintViolation<Education>> violations = validator.validate(filtered.get(i));
                for (ConstraintViolation<Education> violation : violations) {
                    String fieldPath = "items[" + i + "]." + violation.getPropertyPath();
                    bindingResult.addError(new FieldError("form", fieldPath, violation.getMessage()));
                }
            }
        }
        return filtered;
    }

    @Override
    public List<Certificate> prepareCertificates(CertificateForm form, BindingResult bindingResult) {
        List<Certificate> items = form == null || form.getItems() == null ? new ArrayList<>() : form.getItems();
        List<Certificate> filtered = new ArrayList<>();
        for (Certificate item : items) {
            if (!isCertificateEmpty(item)) {
                filtered.add(item);
            }
        }
        if (form != null) {
            form.setItems(filtered);
        }
        if (bindingResult.hasErrors()) {
            return filtered;
        }
        if (!filtered.isEmpty()) {
            for (int i = 0; i < filtered.size(); i++) {
                Set<ConstraintViolation<Certificate>> violations = validator.validate(filtered.get(i));
                for (ConstraintViolation<Certificate> violation : violations) {
                    String fieldPath = "items[" + i + "]." + violation.getPropertyPath();
                    bindingResult.addError(new FieldError("form", fieldPath, violation.getMessage()));
                }
            }
        }
        addDuplicateCertificateErrors(form, bindingResult);
        return filtered;
    }

    @Override
    public void addDuplicateLanguageErrors(LanguageForm form, BindingResult bindingResult) {
        if (form == null || form.getItems() == null || form.getItems().isEmpty()) {
            return;
        }
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
                "language.duplicate",
                null,
                "Language with the same name and type already exists.",
                locale
        );
        Map<String, Integer> seen = new LinkedHashMap<>();
        java.util.Set<Integer> flagged = new java.util.HashSet<>();
        List<net.devstudy.resume.profile.api.model.Language> items = form.getItems();
        for (int i = 0; i < items.size(); i++) {
            net.devstudy.resume.profile.api.model.Language item = items.get(i);
            if (item == null) {
                continue;
            }
            String name = item.getName();
            String normalized = name == null ? "" : name.trim().replaceAll("\\s+", " ");
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            LanguageType type = item.getType() == null ? LanguageType.ALL : item.getType();
            String key = normalized.toLowerCase(Locale.ROOT) + "|" + type.name();
            Integer firstIndex = seen.get(key);
            if (firstIndex != null) {
                String fieldPath = "items[" + i + "].name";
                bindingResult.addError(new FieldError("form", fieldPath, message));
                if (flagged.add(firstIndex)) {
                    String firstFieldPath = "items[" + firstIndex + "].name";
                    bindingResult.addError(new FieldError("form", firstFieldPath, message));
                }
            } else {
                seen.put(key, i);
            }
        }
    }

    private void addDuplicateCertificateErrors(CertificateForm form, BindingResult bindingResult) {
        if (form == null || form.getItems() == null || form.getItems().isEmpty()) {
            return;
        }
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
                "certificate.duplicate",
                null,
                "Certificate with the same name and issuer already exists.",
                locale
        );
        Map<String, Integer> seen = new LinkedHashMap<>();
        java.util.Set<Integer> flagged = new java.util.HashSet<>();
        List<Certificate> items = form.getItems();
        for (int i = 0; i < items.size(); i++) {
            Certificate item = items.get(i);
            if (item == null) {
                continue;
            }
            String nameKey = normalizeCertificateKeyPart(item.getName());
            String issuerKey = normalizeCertificateKeyPart(item.getIssuer());
            if (!StringUtils.hasText(nameKey) || !StringUtils.hasText(issuerKey)) {
                continue;
            }
            String key = nameKey + "|" + issuerKey;
            Integer firstIndex = seen.get(key);
            if (firstIndex != null) {
                String fieldPath = "items[" + i + "].name";
                bindingResult.addError(new FieldError("form", fieldPath, message));
                if (flagged.add(firstIndex)) {
                    String firstFieldPath = "items[" + firstIndex + "].name";
                    bindingResult.addError(new FieldError("form", firstFieldPath, message));
                }
            } else {
                seen.put(key, i);
            }
        }
    }

    private boolean isPracticEmpty(Practic item) {
        if (item == null) {
            return true;
        }
        boolean hasPosition = StringUtils.hasText(item.getPosition());
        boolean hasCompany = StringUtils.hasText(item.getCompany());
        boolean hasResponsibilities = StringUtils.hasText(item.getResponsibilities());
        boolean hasBeginDate = item.getBeginDate() != null;
        return !(hasPosition || hasCompany || hasResponsibilities || hasBeginDate);
    }

    private boolean isEducationEmpty(Education item) {
        if (item == null) {
            return true;
        }
        boolean hasUniversity = StringUtils.hasText(item.getUniversity());
        boolean hasFaculty = StringUtils.hasText(item.getFaculty());
        boolean hasSummary = StringUtils.hasText(item.getSummary());
        return !(hasUniversity || hasFaculty || hasSummary);
    }

    private boolean isCertificateEmpty(Certificate item) {
        if (item == null) {
            return true;
        }
        boolean hasName = StringUtils.hasText(item.getName());
        boolean hasIssuer = StringUtils.hasText(item.getIssuer());
        boolean hasSmallUrl = StringUtils.hasText(item.getSmallUrl());
        boolean hasLargeUrl = StringUtils.hasText(item.getLargeUrl());
        return !(hasName || hasIssuer || hasSmallUrl || hasLargeUrl);
    }

    private String normalizeCertificateKeyPart(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.toLowerCase(Locale.ROOT);
    }
}
