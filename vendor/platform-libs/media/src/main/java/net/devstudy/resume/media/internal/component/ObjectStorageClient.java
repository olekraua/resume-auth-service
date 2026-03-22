package net.devstudy.resume.media.internal.component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.MinioClient.Builder;
import net.devstudy.resume.media.internal.config.ObjectStorageProperties;

@Component
public class ObjectStorageClient {

    private final ObjectStorageProperties properties;
    private final MinioClient minioClient;

    public ObjectStorageClient(ObjectStorageProperties properties) {
        this.properties = properties;
        this.minioClient = createClient(properties);
    }

    @PostConstruct
    public void ensureBucketExists() {
        if (!isEnabled()) {
            return;
        }
        try {
            String bucket = properties.getBucket();
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
            throw new IllegalStateException("Can't initialize object-storage bucket: " + ex.getMessage(), ex);
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public void putObject(String objectKey, Path source, String contentType) {
        requireEnabled();
        String normalizedKey = normalizeObjectKey(objectKey);
        try (InputStream inputStream = Files.newInputStream(source)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(normalizedKey)
                    .stream(inputStream, Files.size(source), -1)
                    .contentType(resolveContentType(contentType))
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("Can't store object '" + normalizedKey + "': " + ex.getMessage(), ex);
        }
    }

    public InputStream getObject(String objectKey) {
        requireEnabled();
        String normalizedKey = normalizeObjectKey(objectKey);
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(normalizedKey)
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("Can't read object '" + normalizedKey + "': " + ex.getMessage(), ex);
        }
    }

    public void removeObjectQuietly(String objectKey) {
        if (!isEnabled()) {
            return;
        }
        String normalizedKey;
        try {
            normalizedKey = normalizeObjectKey(objectKey);
        } catch (IllegalStateException ex) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(normalizedKey)
                    .build());
        } catch (Exception ex) {
            // Best-effort cleanup.
        }
    }

    public String buildPublicUrl(String relativePath) {
        String baseUrl = properties.getPublicBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "/uploads";
        }
        String normalizedBase = stripTrailingSlash(baseUrl.trim());
        String normalizedPath = normalizeRelativePath(relativePath);
        if (!normalizedBase.startsWith("http://") && !normalizedBase.startsWith("https://")
                && !normalizedBase.startsWith("/")) {
            normalizedBase = "/" + normalizedBase;
        }
        return normalizedBase + "/" + normalizedPath;
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

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new IllegalStateException("Object storage is disabled");
        }
    }

    private String normalizeObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new IllegalStateException("Object key is blank");
        }
        String normalized = objectKey.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!StringUtils.hasText(normalized) || normalized.contains("..")) {
            throw new IllegalStateException("Object key is invalid");
        }
        return normalized;
    }

    private String normalizeRelativePath(String relativePath) {
        String normalized = normalizeObjectKey(relativePath);
        if (normalized.startsWith("uploads/")) {
            return normalized.substring("uploads/".length());
        }
        return normalized;
    }

    private String stripTrailingSlash(String value) {
        String normalized = value;
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if ("/".equals(normalized)) {
            return "";
        }
        return normalized;
    }

    private String resolveContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "application/octet-stream";
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }
}
