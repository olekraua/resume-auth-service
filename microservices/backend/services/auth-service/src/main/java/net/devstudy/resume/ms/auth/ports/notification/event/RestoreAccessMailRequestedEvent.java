package net.devstudy.resume.ms.auth.ports.notification.event;

public record RestoreAccessMailRequestedEvent(String email, String firstName, String link) {
}
