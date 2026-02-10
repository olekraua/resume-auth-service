package net.devstudy.resume.shared.component.impl;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import net.devstudy.resume.shared.component.TranslitConverter;
import net.sf.junidecode.Junidecode;

@Component
@Primary
public class JunidecodeTranslitConverter implements TranslitConverter {

    @Override
    public String translit(String text) {
        return text == null ? "" : Junidecode.unidecode(text);
    }
}
