package net.devstudy.resume.notification.api.messaging;

public final class NotificationMessaging {
    public static final String EXCHANGE = "resume.notification";
    public static final String QUEUE = "resume.notification.mail";
    public static final String ROUTING_KEY_RESTORE = "auth.restore-mail";
    public static final String RETRY_EXCHANGE = "resume.notification.retry";
    public static final String RETRY_QUEUE = "resume.notification.mail.retry";
    public static final String RETRY_ROUTING_KEY = "auth.restore-mail.retry";
    public static final String DLX_EXCHANGE = "resume.notification.dlx";
    public static final String DLQ = "resume.notification.mail.dlq";
    public static final String DLQ_ROUTING_KEY = "auth.restore-mail.dlq";

    private NotificationMessaging() {
    }
}
