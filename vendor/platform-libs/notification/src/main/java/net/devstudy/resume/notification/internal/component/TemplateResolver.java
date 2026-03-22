package net.devstudy.resume.notification.internal.component;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface TemplateResolver {

    @NonNull
    String resolve(@NonNull String template, @Nullable Object model);
}
