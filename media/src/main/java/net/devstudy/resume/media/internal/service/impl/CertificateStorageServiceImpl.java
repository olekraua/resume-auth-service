package net.devstudy.resume.media.internal.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import net.devstudy.resume.shared.component.DataBuilder;
import net.devstudy.resume.media.internal.component.ImageFormatConverter;
import net.devstudy.resume.media.internal.component.ImageOptimizator;
import net.devstudy.resume.media.internal.component.ImageResizer;
import net.devstudy.resume.media.internal.component.impl.UploadImageTempStorage;
import net.devstudy.resume.media.internal.component.impl.UploadCertificateLinkTempStorage;
import net.devstudy.resume.media.internal.config.CertificateUploadProperties;
import net.devstudy.resume.media.api.dto.UploadCertificateResult;
import net.devstudy.resume.media.internal.model.UploadTempPath;
import net.devstudy.resume.media.api.service.CertificateStorageService;

@Service
public class CertificateStorageServiceImpl implements CertificateStorageService {

    private final DataBuilder dataBuilder;
    private final ImageOptimizator imageOptimizator;
    private final ImageFormatConverter pngToJpegImageFormatConverter;
    private final ImageResizer imageResizer;
    private final CertificateUploadProperties certificateUploadProperties;
    private final UploadCertificateLinkTempStorage uploadCertificateLinkTempStorage;
    private final UploadImageTempStorage uploadImageTempStorage;
    private final Executor mediaOptimizationExecutor;

    public CertificateStorageServiceImpl(DataBuilder dataBuilder,
            ImageOptimizator imageOptimizator,
            ImageFormatConverter pngToJpegImageFormatConverter,
            ImageResizer imageResizer,
            CertificateUploadProperties certificateUploadProperties,
            UploadCertificateLinkTempStorage uploadCertificateLinkTempStorage,
            UploadImageTempStorage uploadImageTempStorage,
            @Qualifier("mediaOptimizationExecutor") Executor mediaOptimizationExecutor) {
        this.dataBuilder = dataBuilder;
        this.imageOptimizator = imageOptimizator;
        this.pngToJpegImageFormatConverter = pngToJpegImageFormatConverter;
        this.imageResizer = imageResizer;
        this.certificateUploadProperties = certificateUploadProperties;
        this.uploadCertificateLinkTempStorage = uploadCertificateLinkTempStorage;
        this.uploadImageTempStorage = uploadImageTempStorage;
        this.mediaOptimizationExecutor = mediaOptimizationExecutor;
    }

    @Override
    @EnableUploadImageTempStorage
    public UploadCertificateResult store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty certificate file");
        }
        UploadTempPath uploadTempPath = resolveUploadTempPath();
        boolean cleanupTempFiles = uploadTempPath != uploadImageTempStorage.getCurrentUploadTempPath();
        try {
            byte[] data = file.getBytes();
            validateCertificate(file, data);

            Path dir = Path.of(certificateUploadProperties.getDir());
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
                Path tmp = Files.createTempFile("resume-certificate-", ".png");
                try {
                    Files.write(tmp, data, StandardOpenOption.TRUNCATE_EXISTING);
                    pngToJpegImageFormatConverter.convert(tmp, tempLarge);
                } finally {
                    Files.deleteIfExists(tmp);
                }
            } else {
                Files.write(tempLarge, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            resizeCertificate(tempLarge, tempSmall);
            optimizeInParallel(tempLarge, tempSmall);
            Files.copy(tempLarge, largeTarget, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(tempSmall, smallTarget, StandardCopyOption.REPLACE_EXISTING);

            String largeUrl = "/uploads/certificates/" + fileName;
            String smallUrl = "/uploads/certificates/" + smallName;
            String certName = dataBuilder.buildCertificateName(file.getOriginalFilename());
            uploadCertificateLinkTempStorage.addImageLinks(largeUrl, smallUrl);
            return new UploadCertificateResult(certName, largeUrl, smallUrl);
        } catch (IOException e) {
            throw new RuntimeException("Can't store certificate file", e);
        } finally {
            if (cleanupTempFiles) {
                deleteQuietly(uploadTempPath.getLargeImagePath());
                deleteQuietly(uploadTempPath.getSmallImagePath());
            }
        }
    }

    private void resizeCertificate(Path largeTarget, Path smallTarget) throws IOException {
        imageResizer.resize(largeTarget, smallTarget,
                certificateUploadProperties.getSmallWidth(),
                certificateUploadProperties.getSmallHeight());
        imageResizer.resize(largeTarget, largeTarget,
                certificateUploadProperties.getLargeWidth(),
                certificateUploadProperties.getLargeHeight());
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

    private void validateCertificate(MultipartFile file, byte[] data) {
        String ext = getExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png"))) {
            throw new IllegalArgumentException("Only jpg or png images are allowed");
        }
        try {
            var img = javax.imageio.ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) {
                throw new IllegalArgumentException("Invalid image format");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't read image", e);
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
