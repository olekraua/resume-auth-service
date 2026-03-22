package net.devstudy.resume.auth.api.component;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface AuthDataBuilder {

    @NonNull
    String buildProfileUid(@Nullable String firstName, @Nullable String lastName);

    @NonNull
    String buildRestoreAccessLink(@NonNull String appHost, @NonNull String token);

    @NonNull
    String rebuildUidWithRandomSuffix(@NonNull String baseUid, @NonNull String alphabet, int letterCount);
}
