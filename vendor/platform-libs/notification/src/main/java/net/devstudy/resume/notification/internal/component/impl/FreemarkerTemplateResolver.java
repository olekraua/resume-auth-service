package net.devstudy.resume.notification.internal.component.impl;

import java.io.IOException;
import java.io.StringReader;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.devstudy.resume.notification.internal.component.TemplateResolver;

@Component
public class FreemarkerTemplateResolver implements TemplateResolver {

    private static final Configuration CONFIGURATION = new Configuration(Configuration.VERSION_2_3_0);

    @Override
    public @NonNull String resolve(@NonNull String template, @Nullable Object model) {
        try {
            Template freemarkerTemplate = new Template("", new StringReader(template), CONFIGURATION);
            return FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerTemplate, model);
        } catch (IOException | TemplateException ex) {
            throw new IllegalArgumentException("Can't resolve string template: " + ex.getMessage(), ex);
        }
    }
}
