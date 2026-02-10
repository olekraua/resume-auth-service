package net.devstudy.resume.media.internal.component;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.devstudy.resume.media.internal.config.PhotoUploadProperties;

@Component
public class PhotoFileStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhotoFileStorage.class);
    private static final String URL_PREFIX = "/uploads/photos/";

    private final PhotoUploadProperties photoUploadProperties;

    public PhotoFileStorage(PhotoUploadProperties photoUploadProperties) {
        this.photoUploadProperties = photoUploadProperties;
    }

    public void removeAll(Collection<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        Path baseDir = Path.of(photoUploadProperties.getDir()).toAbsolutePath().normalize();
        Set<String> uniqueUrls = new LinkedHashSet<>(urls);
        for (String url : uniqueUrls) {
            Path target = resolveTargetPath(url, baseDir);
            if (target == null) {
                continue;
            }
            try {
                if (Files.deleteIfExists(target)) {
                    LOGGER.debug("Removed photo {}", target);
                }
            } catch (Exception ex) {
                LOGGER.warn("Can't remove photo {}: {}", target, ex.getMessage());
            }
        }
    }

    private Path resolveTargetPath(String url, Path baseDir) {
        String path = extractPath(url);
        if (path == null) {
            return null;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.startsWith(URL_PREFIX)) {
            return null;
        }
        String fileName = path.substring(URL_PREFIX.length());
        if (fileName.isBlank() || fileName.contains("/")) {
            return null;
        }
        Path target = baseDir.resolve(fileName).normalize();
        if (!target.startsWith(baseDir)) {
            return null;
        }
        return target;
    }

    private String extractPath(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            return URI.create(url).getPath();
        } catch (IllegalArgumentException ex) {
            return url;
        }
    }
}
