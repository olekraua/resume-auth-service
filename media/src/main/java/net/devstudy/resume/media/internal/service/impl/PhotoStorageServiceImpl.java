package net.devstudy.resume.media.internal.service.impl;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.devstudy.resume.media.internal.annotation.EnableUploadImageTempStorage;
import net.devstudy.resume.media.internal.component.ImageFormatConverter;
import net.devstudy.resume.media.internal.component.ImageOptimizator;
import net.devstudy.resume.media.internal.component.ImageResizer;
import net.devstudy.resume.media.internal.component.impl.UploadImageTempStorage;
import net.devstudy.resume.media.internal.config.PhotoUploadProperties;
import net.devstudy.resume.media.internal.model.UploadTempPath;
import net.devstudy.resume.media.api.service.PhotoStorageService;

@Service
public class PhotoStorageServiceImpl implements PhotoStorageService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final int MIN_DIMENSION = 400;

    private final ImageOptimizator imageOptimizator;
    private final ImageFormatConverter pngToJpegImageFormatConverter;
    private final ImageResizer imageResizer;
    private final UploadImageTempStorage uploadImageTempStorage;
    private final PhotoUploadProperties photoUploadProperties;
    private final Executor mediaOptimizationExecutor;

    public PhotoStorageServiceImpl(ImageOptimizator imageOptimizator,
            ImageFormatConverter pngToJpegImageFormatConverter,
            ImageResizer imageResizer,
            UploadImageTempStorage uploadImageTempStorage,
            PhotoUploadProperties photoUploadProperties,
            @Qualifier("mediaOptimizationExecutor") Executor mediaOptimizationExecutor) {
        this.imageOptimizator = imageOptimizator;
        this.pngToJpegImageFormatConverter = pngToJpegImageFormatConverter;
        this.imageResizer = imageResizer;
        this.uploadImageTempStorage = uploadImageTempStorage;
        this.photoUploadProperties = photoUploadProperties;
        this.mediaOptimizationExecutor = mediaOptimizationExecutor;
    }

    @Override
    @EnableUploadImageTempStorage
    public String[] store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty photo file");
        }
        UploadTempPath uploadTempPath = resolveUploadTempPath();
        boolean cleanupTempFiles = uploadTempPath != uploadImageTempStorage.getCurrentUploadTempPath();
        try {
            byte[] data = file.getBytes();
            validatePhoto(file, data);

            Path dir = Path.of(photoUploadProperties.getDir());
            Files.createDirectories(dir);
            String ext = getExtension(file.getOriginalFilename());
            boolean isPng = isPng(file, ext);
            String targetExt = isPng ? "jpg" : ext;
            String baseName = UUID.randomUUID().toString();
            String fileName = baseName + (targetExt.isEmpty() ? "" : "." + targetExt);
            String smallName = baseName + "-sm" + (targetExt.isEmpty() ? "" : "." + targetExt);

            Path largeTarget = dir.resolve(fileName);
            Path smallTarget = dir.resolve(smallName);
            Path tempLarge = uploadTempPath.getLargeImagePath();
            Path tempSmall = uploadTempPath.getSmallImagePath();

            if (isPng) {
                Path tmp = Files.createTempFile("resume-photo-", ".png");
                try {
                    Files.write(tmp, data, StandardOpenOption.TRUNCATE_EXISTING);
                    pngToJpegImageFormatConverter.convert(tmp, tempLarge);
                } finally {
                    Files.deleteIfExists(tmp);
                }
            } else {
                Files.write(tempLarge, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            resizePhoto(tempLarge, tempSmall);
            optimizeInParallel(tempLarge, tempSmall);
            Files.copy(tempLarge, largeTarget, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(tempSmall, smallTarget, StandardCopyOption.REPLACE_EXISTING);

            String baseUrl = "/uploads/photos/";
            return new String[] { baseUrl + fileName, baseUrl + smallName };
        } catch (IOException e) {
            throw new RuntimeException("Can't store photo file", e);
        } finally {
            if (cleanupTempFiles) {
                deleteQuietly(uploadTempPath.getLargeImagePath());
                deleteQuietly(uploadTempPath.getSmallImagePath());
            }
        }
    }

    private void resizePhoto(Path largeTarget, Path smallTarget) throws IOException {
        imageResizer.resize(largeTarget, smallTarget,
                photoUploadProperties.getSmallWidth(),
                photoUploadProperties.getSmallHeight());
        imageResizer.resize(largeTarget, largeTarget,
                photoUploadProperties.getLargeWidth(),
                photoUploadProperties.getLargeHeight());
    }

    private void optimizeInParallel(Path largeTarget, Path smallTarget) {
        CompletableFuture<Void> largeTask = CompletableFuture.runAsync(
                () -> imageOptimizator.optimize(largeTarget),
                mediaOptimizationExecutor);
        CompletableFuture<Void> smallTask = CompletableFuture.runAsync(
                () -> imageOptimizator.optimize(smallTarget),
                mediaOptimizationExecutor);
        CompletableFuture.allOf(largeTask, smallTask).join();
    }

    private UploadTempPath resolveUploadTempPath() {
        UploadTempPath uploadTempPath = uploadImageTempStorage.getCurrentUploadTempPath();
        if (uploadTempPath != null) {
            return uploadTempPath;
        }
        try {
            return new UploadTempPath();
        } catch (IOException ex) {
            throw new IllegalStateException("Can't create temp image files: " + ex.getMessage(), ex);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            // ignore: best-effort cleanup
        }
    }

    private void validatePhoto(MultipartFile file, byte[] data) {
        if (data.length > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Фото завелике: максимум 5MB");
        }
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        if (!(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png"))) {
            throw new IllegalArgumentException("Дозволені лише jpg або png");
        }
        try {
            var img = javax.imageio.ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) {
                throw new IllegalArgumentException("Неправильний формат зображення");
            }
            if (img.getWidth() < MIN_DIMENSION || img.getHeight() < MIN_DIMENSION) {
                throw new IllegalArgumentException("Мінімальний розмір фото 400x400");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Неможливо прочитати зображення", e);
        }
    }

    private String getExtension(String name) {
        if (name == null) return "";
        int idx = name.lastIndexOf('.');
        return (idx >= 0 && idx < name.length() - 1) ? name.substring(idx + 1) : "";
    }

    private boolean isPng(MultipartFile file, String ext) {
        if ("png".equalsIgnoreCase(ext)) {
            return true;
        }
        String contentType = file.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).contains("png");
    }
}
