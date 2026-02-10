package net.devstudy.resume.media.api.service;

import java.util.Collection;

public interface MediaCleanupService {
    void removePhotos(Collection<String> urls);

    void removeCertificates(Collection<String> urls);

    void clearCertificateTempLinks();
}
