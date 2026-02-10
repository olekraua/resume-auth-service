package net.devstudy.resume.media.internal.component;

import java.io.IOException;

import net.devstudy.resume.media.internal.model.UploadTempPath;

public interface UploadTempPathFactory {

    UploadTempPath create() throws IOException;
}
