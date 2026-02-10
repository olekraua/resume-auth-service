package net.devstudy.resume.media.internal.component.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import net.devstudy.resume.media.internal.component.CertificateFileStorage;

@Component
@Scope(scopeName = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UploadCertificateLinkTempStorage implements Serializable {

    private static final long serialVersionUID = -8075703850628908992L;
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadCertificateLinkTempStorage.class);

    private final CertificateFileStorage certificateFileStorage;

    private List<String> imageLinks;

    public UploadCertificateLinkTempStorage(CertificateFileStorage certificateFileStorage) {
        this.certificateFileStorage = certificateFileStorage;
    }

    protected List<String> getImageLinks() {
        if (imageLinks == null) {
            imageLinks = new ArrayList<>(6);
        }
        return imageLinks;
    }

    public void addImageLinks(String largeImageLink, String smallImageLink) {
        getImageLinks().add(largeImageLink);
        getImageLinks().add(smallImageLink);
    }

    public void clearImageLinks() {
        getImageLinks().clear();
    }

    @PreDestroy
    private void preDestroy() {
        if (!getImageLinks().isEmpty()) {
            certificateFileStorage.removeAll(new ArrayList<>(getImageLinks()));
            LOGGER.info("Removed {} temporary certificate images", getImageLinks().size());
        }
    }
}
