package net.devstudy.resume.file.api.messaging;

public final class FilePlatformMessaging {

    public static final String TOPIC_UPLOADED = "resume.file.uploaded";
    public static final String TOPIC_ANTIVIRUS_PASSED = "resume.file.antivirus.passed";
    public static final String TOPIC_ANTIVIRUS_FAILED = "resume.file.antivirus.failed";
    public static final String TOPIC_MODERATION_PASSED = "resume.file.moderation.passed";
    public static final String TOPIC_MODERATION_FAILED = "resume.file.moderation.failed";
    public static final String TOPIC_THUMBNAIL_READY = "resume.file.thumbnail.ready";
    public static final String TOPIC_THUMBNAIL_FAILED = "resume.file.thumbnail.failed";
    public static final String TOPIC_READY = "resume.file.ready";
    public static final String TOPIC_FAILED = "resume.file.failed";
    public static final String TOPIC_DELETED = "resume.file.deleted";

    public static final String CONSUMER_GROUP_WORKER = "resume-file-worker";
    public static final String CONSUMER_GROUP_ANTIVIRUS = "resume-file-antivirus-worker";
    public static final String CONSUMER_GROUP_MODERATION = "resume-file-moderation-worker";
    public static final String CONSUMER_GROUP_THUMBNAIL = "resume-file-thumbnail-worker";
    public static final String CONSUMER_GROUP_CLEANUP = "resume-file-cleanup-worker";

    private FilePlatformMessaging() {
    }
}
