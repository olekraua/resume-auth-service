package net.devstudy.resume.media.api.service;

import org.springframework.web.multipart.MultipartFile;

import net.devstudy.resume.media.api.dto.UploadCertificateResult;

public interface CertificateStorageService {
    UploadCertificateResult store(MultipartFile file);
}
