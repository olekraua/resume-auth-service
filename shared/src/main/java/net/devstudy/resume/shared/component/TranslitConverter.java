package net.devstudy.resume.shared.component;

import org.springframework.lang.NonNull;

public interface TranslitConverter {

    @NonNull
    String translit(@NonNull String text);
}
