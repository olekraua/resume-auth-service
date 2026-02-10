package net.devstudy.resume.notification.internal.service;

public interface RestoreAccessMailService {

    void sendRestoreLink(String email, String firstName, String link);
}
