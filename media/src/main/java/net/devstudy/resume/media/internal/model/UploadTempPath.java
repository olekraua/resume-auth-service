package net.devstudy.resume.media.internal.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.devstudy.resume.shared.model.AbstractModel;

public class UploadTempPath extends AbstractModel {

    private final Path largeImagePath;
    private final Path smallImagePath;

    public UploadTempPath() throws IOException {
        largeImagePath = Files.createTempFile("large", ".jpg");
        smallImagePath = Files.createTempFile("small", ".jpg");
    }

    public Path getLargeImagePath() {
        return largeImagePath;
    }

    public Path getSmallImagePath() {
        return smallImagePath;
    }
}
