package net.devstudy.resume.media.internal.component.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import net.devstudy.resume.media.internal.component.ImageOptimizator;

@Component
public class JpegTranImageOptimizator implements ImageOptimizator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpegTranImageOptimizator.class);
    private static final List<String> TOOL_OPTIONS = List.of("-copy", "none", "-optimize", "-progressive");

    @Value("${media.optimization.jpegtran:}")
    private String jpegtran;

    private final AtomicBoolean missingToolLogged = new AtomicBoolean(false);

    @Override
    public void optimize(Path imageFile) {
        if (imageFile == null || !isJpeg(imageFile)) {
            return;
        }
        if (!isToolAvailable()) {
            return;
        }
        try {
            optimizeImageFile(imageFile);
        } catch (IOException ex) {
            LOGGER.warn("Can't optimize image file {}: {}", imageFile, ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Image optimization interrupted for {}: {}", imageFile, ex.getMessage());
        }
    }

    private void optimizeImageFile(Path imageFile) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(TOOL_OPTIONS.size() + 4);
        command.add(jpegtran);
        command.addAll(TOOL_OPTIONS);
        command.add("-outfile");
        command.add(imageFile.toString());
        command.add(imageFile.toString());

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        String output;
        try (InputStream stream = process.getInputStream()) {
            output = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
        }
        int code;
        try {
            code = process.waitFor();
        } finally {
            process.destroy();
        }
        if (code != 0) {
            String message = output == null ? "" : output.trim();
            LOGGER.warn("jpegtran failed for {} (code={}): {}", imageFile, code, message);
        } else {
            LOGGER.debug("jpegtran optimized {}", imageFile);
        }
    }

    private boolean isToolAvailable() {
        if (jpegtran == null || jpegtran.isBlank()) {
            logMissingTool("media.optimization.jpegtran is blank");
            return false;
        }
        Path toolPath = Path.of(jpegtran);
        if (toolPath.getParent() == null) {
            return true;
        }
        if (!Files.exists(toolPath)) {
            logMissingTool("jpegtran not found at " + jpegtran);
            return false;
        }
        if (!Files.isExecutable(toolPath)) {
            logMissingTool("jpegtran is not executable at " + jpegtran);
            return false;
        }
        return true;
    }

    private void logMissingTool(String reason) {
        if (missingToolLogged.compareAndSet(false, true)) {
            LOGGER.warn("Image optimization disabled: {}", reason);
        }
    }

    private boolean isJpeg(Path imageFile) {
        String name = imageFile.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg");
    }
}
