package net.devstudy.resume.file.api.event;

public final class FileEventTypes {

    public static final String FILE_UPLOADED_V1 = "file.uploaded.v1";
    public static final String FILE_ANTIVIRUS_PASSED_V1 = "file.antivirus.passed.v1";
    public static final String FILE_ANTIVIRUS_FAILED_V1 = "file.antivirus.failed.v1";
    public static final String FILE_MODERATION_PASSED_V1 = "file.moderation.passed.v1";
    public static final String FILE_MODERATION_FAILED_V1 = "file.moderation.failed.v1";
    public static final String FILE_THUMBNAIL_READY_V1 = "file.thumbnail.ready.v1";
    public static final String FILE_THUMBNAIL_FAILED_V1 = "file.thumbnail.failed.v1";
    public static final String FILE_READY_V1 = "file.ready.v1";
    public static final String FILE_FAILED_V1 = "file.failed.v1";
    public static final String FILE_DELETED_V1 = "file.deleted.v1";

    private FileEventTypes() {
    }
}
