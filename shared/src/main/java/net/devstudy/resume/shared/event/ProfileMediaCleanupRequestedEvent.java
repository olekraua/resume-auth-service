package net.devstudy.resume.shared.event;

import java.util.List;

public record ProfileMediaCleanupRequestedEvent(List<String> photoUrls, List<String> certificateUrls,
        boolean clearCertificateTempLinks) {

    public ProfileMediaCleanupRequestedEvent {
        photoUrls = photoUrls == null ? List.of() : List.copyOf(photoUrls);
        certificateUrls = certificateUrls == null ? List.of() : List.copyOf(certificateUrls);
    }
}
