package net.devstudy.resume.shared.component.impl;

import net.devstudy.resume.shared.component.TranslitConverter;
import net.sf.junidecode.Junidecode;

public class JunidecodeTranslitConverter implements TranslitConverter {

    @Override
    public String translit(String text) {
        return text == null ? "" : Junidecode.unidecode(text);
    }
}
