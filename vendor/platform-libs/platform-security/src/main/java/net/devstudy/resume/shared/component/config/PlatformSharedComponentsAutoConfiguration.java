package net.devstudy.resume.shared.component.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import net.devstudy.resume.shared.component.TranslitConverter;
import net.devstudy.resume.shared.component.impl.JunidecodeTranslitConverter;
import net.devstudy.resume.shared.component.impl.SimpleTranslitConverter;

@AutoConfiguration
public class PlatformSharedComponentsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TranslitConverter.class)
    @ConditionalOnClass(name = "net.sf.junidecode.Junidecode")
    public TranslitConverter junidecodeTranslitConverter() {
        return new JunidecodeTranslitConverter();
    }

    @Bean
    @ConditionalOnMissingBean(TranslitConverter.class)
    public TranslitConverter simpleTranslitConverter() {
        return new SimpleTranslitConverter();
    }
}
