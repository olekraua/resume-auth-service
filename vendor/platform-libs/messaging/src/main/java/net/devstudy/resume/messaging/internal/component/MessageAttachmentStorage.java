package net.devstudy.resume.messaging.internal.component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.MinioClient.Builder;
import io.minio.PutObjectArgs;
import net.devstudy.resume.messaging.internal.config.MessageAttachmentProperties;
import net.devstudy.resume.messaging.internal.config.ObjectStorageProperties;

@Component
public class MessageAttachmentStorage {

    private final MessageAttachmentProperties properties;
    private final ObjectStorageProperties objectStorageProperties;
    private final MinioClient minioClient;

    public MessageAttachmentStorage(MessageAttachmentProperties properties,
            ObjectStorageProperties objectStorageProperties) {
        this.properties = properties;
        this.objectStorageProperties = objectStorageProperties;
        this.minioClient = createClient(objectStorageProperties);
    }

    @PostConstruct
    public void ensureBucketExists() {
        if (!objectStorageProperties.isEnabled()) {
            return;
        }
        try {
            String bucket = objectStorageProperties.getBucket();
            BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucket).build();
            boolean exists = minioClient.bucketExists(bucketExistsArgs);
            if (!exists) {
                try {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                } catch (Exception ex) {
                    if (!minioClient.bucketExists(bucketExistsArgs)) {
                        throw ex;
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize object-storage bucket", ex);
        }
    }

    public StoredAttachment store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Attachment is empty");
        }
        String contentType = normalizeContentType(file.getContentType());
        String extension = resolveExtension(contentType);
        String storageKey = generateStorageKey(extension);
        if (objectStorageProperties.isEnabled()) {
            storeInObjectStorage(file, storageKey, contentType);
        } else {
            storeInFileSystem(file, storageKey);
        }
        String originalName = normalizeOriginalName(file.getOriginalFilename());
        return new StoredAttachment(storageKey, originalName, contentType, file.getSize());
    }

    public Resource resolveResource(String storageKey) {
        String normalizedStorageKey = normalizeStorageKey(storageKey);
        if (normalizedStorageKey == null) {
            return null;
        }
        if (objectStorageProperties.isEnabled()) {
            return resolveObjectResource(normalizedStorageKey);
        }
        Path path = resolveFilePath(normalizedStorageKey);
        if (path == null || !Files.exists(path)) {
            return null;
        }
        return new FileSystemResource(path);
    }

    private void storeInObjectStorage(MultipartFile file, String storageKey, String contentType) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(objectStorageProperties.getBucket())
                    .object(toObjectKey(storageKey))
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to store attachment in object storage", ex);
        }
    }

    private void storeInFileSystem(MultipartFile file, String storageKey) {
        Path baseDir = resolveBaseDir();
        Path target = baseDir.resolve(storageKey).normalize();
        if (!target.startsWith(baseDir)) {
            throw new IllegalStateException("Invalid attachment path");
        }
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store attachment", ex);
        }
    }

    private Path resolveFilePath(String storageKey) {
        Path baseDir = resolveBaseDir();
        Path target = baseDir.resolve(storageKey).normalize();
        if (!target.startsWith(baseDir)) {
            return null;
        }
        return target;
    }

    private Path resolveBaseDir() {
        Path baseDir = Path.of(properties.getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create attachment directory", ex);
        }
        return baseDir;
    }

    private String generateStorageKey(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (extension == null || extension.isBlank()) {
            return uuid;
        }
        return uuid + extension;
    }

    private String resolveExtension(String contentType) {
        if (contentType == null) {
            return "";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }

    private String normalizeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "attachment";
        }
        String trimmed = originalName.trim();
        if (trimmed.length() > 255) {
            return trimmed.substring(trimmed.length() - 255);
        }
        return trimmed;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "application/octet-stream";
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private Resource resolveObjectResource(String storageKey) {
        try {
            InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(objectStorageProperties.getBucket())
                    .object(toObjectKey(storageKey))
                    .build());
            return new InputStreamResource(inputStream);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeStorageKey(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            return null;
        }
        String normalized = storageKey.trim();
        if (normalized.contains("/") || normalized.contains("\\") || normalized.contains("..")) {
            return null;
        }
        return normalized;
    }

    private String toObjectKey(String storageKey) {
        return "messages/" + storageKey;
    }

    private MinioClient createClient(ObjectStorageProperties props) {
        if (!props.isEnabled()) {
            return null;
        }
        Builder builder = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey());
        if (StringUtils.hasText(props.getRegion())) {
            builder.region(props.getRegion());
        }
        return builder.build();
    }

    public record StoredAttachment(String storageKey, String originalName, String contentType, long size) {
    }
}
