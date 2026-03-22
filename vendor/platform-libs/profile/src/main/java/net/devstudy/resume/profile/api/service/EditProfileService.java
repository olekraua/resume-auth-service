package net.devstudy.resume.profile.api.service;

import java.util.List;

import org.springframework.validation.BindingResult;

import net.devstudy.resume.profile.api.dto.CertificateForm;
import net.devstudy.resume.profile.api.dto.ContactsForm;
import net.devstudy.resume.profile.api.dto.EducationForm;
import net.devstudy.resume.profile.api.dto.InfoForm;
import net.devstudy.resume.profile.api.dto.LanguageForm;
import net.devstudy.resume.profile.api.dto.PracticForm;
import net.devstudy.resume.profile.api.dto.ProfileMainForm;
import net.devstudy.resume.profile.api.model.Certificate;
import net.devstudy.resume.profile.api.model.Education;
import net.devstudy.resume.profile.api.model.Practic;
import net.devstudy.resume.profile.api.model.Profile;

public interface EditProfileService {

    ProfileMainForm toProfileMainForm(Profile profile);

    InfoForm toInfoForm(ProfileMainForm form);

    ContactsForm toContactsForm(ProfileMainForm form);

    Object formFromProfile(Object emptyForm, Profile profile);

    List<Practic> preparePractics(PracticForm form, BindingResult bindingResult);

    List<Education> prepareEducations(EducationForm form, BindingResult bindingResult);

    List<Certificate> prepareCertificates(CertificateForm form, BindingResult bindingResult);

    void addDuplicateLanguageErrors(LanguageForm form, BindingResult bindingResult);
}
