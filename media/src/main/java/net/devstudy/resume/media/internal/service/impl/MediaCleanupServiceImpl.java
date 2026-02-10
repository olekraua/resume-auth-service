package net.devstudy.resume.media.internal.service.impl;

import java.util.Collection;

import org.springframework.stereotype.Service;

import net.devstudy.resume.media.api.service.MediaCleanupService;
import net.devstudy.resume.media.internal.component.CertificateFileStorage;
import net.devstudy.resume.media.internal.component.PhotoFileStorage;
import net.devstudy.resume.media.internal.component.impl.UploadCertificateLinkTempStorage;

@Service
public class MediaCleanupServiceImpl implements MediaCleanupService {

    private final PhotoFileStorage photoFileStorage;
    private final CertificateFileStorage certificateFileStorage;
    private final UploadCertificateLinkTempStorage uploadCertificateLinkTempStorage;

    public MediaCleanupServiceImpl(PhotoFileStorage photoFileStorage,
            CertificateFileStorage certificateFileStorage,
            UploadCertificateLinkTempStorage uploadCertificateLinkTempStorage) {
        this.photoFileStorage = photoFileStorage;
        this.certificateFileStorage = certificateFileStorage;
        this.uploadCertificateLinkTempStorage = uploadCertificateLinkTempStorage;
    }

    @Override
    public void removePhotos(Collection<String> urls) {
        photoFileStorage.removeAll(urls);
    }

    @Override
    public void removeCertificates(Collection<String> urls) {
        certificateFileStorage.removeAll(urls);
    }

    @Override
    public void clearCertificateTempLinks() {
        uploadCertificateLinkTempStorage.clearImageLinks();
    }
}
