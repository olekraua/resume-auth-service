package net.devstudy.resume.notification.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.restore.mail.template")
public class RestoreMailTemplateProperties {

    private String text = "classpath:/mail/restore-access.txt.ftl";
    private String html = "classpath:/mail/restore-access.html.ftl";

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }
}
