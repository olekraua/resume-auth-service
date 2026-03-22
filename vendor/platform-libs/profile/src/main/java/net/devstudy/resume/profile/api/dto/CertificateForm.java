package net.devstudy.resume.profile.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import net.devstudy.resume.profile.api.model.Certificate;

@Getter
@Setter
public class CertificateForm {
    @Valid
    private List<Certificate> items;
}
