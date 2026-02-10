package net.devstudy.resume.media.internal.component;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.lang.NonNull;

public interface ImageResizer {

    void resize(@NonNull Path sourceImageFile, @NonNull Path destImageFile, int width, int height)
            throws IOException;
}
