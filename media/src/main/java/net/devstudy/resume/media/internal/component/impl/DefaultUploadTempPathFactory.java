package net.devstudy.resume.media.internal.component.impl;

import java.io.IOException;

import org.springframework.stereotype.Component;

import net.devstudy.resume.media.internal.component.UploadTempPathFactory;
import net.devstudy.resume.media.internal.model.UploadTempPath;

@Component
public class DefaultUploadTempPathFactory implements UploadTempPathFactory {

    @Override
    public UploadTempPath create() throws IOException {
        return new UploadTempPath();
    }
}
