package net.devstudy.resume.notification.api.event;

public record RestoreAccessMailRequestedEvent(String email, String firstName, String link) {
}
