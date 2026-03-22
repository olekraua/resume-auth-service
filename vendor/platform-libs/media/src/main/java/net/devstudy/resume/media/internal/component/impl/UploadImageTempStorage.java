package net.devstudy.resume.media.internal.component.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import net.devstudy.resume.media.internal.annotation.EnableUploadImageTempStorage;
import net.devstudy.resume.media.internal.component.UploadTempPathFactory;
import net.devstudy.resume.media.internal.model.UploadTempPath;

@Aspect
@Component
public class UploadImageTempStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadImageTempStorage.class);

    private final ThreadLocal<UploadTempPath> currentUploadTempPath = new ThreadLocal<>();
    private final UploadTempPathFactory uploadTempPathFactory;

    public UploadImageTempStorage(UploadTempPathFactory uploadTempPathFactory) {
        this.uploadTempPathFactory = uploadTempPathFactory;
    }

    @Around("@annotation(enableUploadImageTempStorage)")
    public Object advice(ProceedingJoinPoint pjp, EnableUploadImageTempStorage enableUploadImageTempStorage)
            throws Throwable {
        UploadTempPath uploadTempPath;
        try {
            uploadTempPath = uploadTempPathFactory.create();
        } catch (IOException ex) {
            throw new IllegalStateException("Can't create temp image files: " + ex.getMessage(), ex);
        }

        try {
            currentUploadTempPath.set(uploadTempPath);
            LOGGER.debug("Before method: {}", pjp.getSignature());
            return pjp.proceed();
        } finally {
            LOGGER.debug("After method: {}", pjp.getSignature());
            currentUploadTempPath.remove();
            deleteQuietly(uploadTempPath.getLargeImagePath());
            deleteQuietly(uploadTempPath.getSmallImagePath());
        }
    }

    public UploadTempPath getCurrentUploadTempPath() {
        return currentUploadTempPath.get();
    }

    protected void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
            LOGGER.debug("Delete temp file {} successful", path);
        } catch (IOException ex) {
            LOGGER.warn("Can't remove temp file: {}", path, ex);
        }
    }
}
