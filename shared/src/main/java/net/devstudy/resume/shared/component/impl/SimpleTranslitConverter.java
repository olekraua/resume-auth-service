package net.devstudy.resume.shared.component.impl;

import java.util.Map;

import org.springframework.stereotype.Component;

import net.devstudy.resume.shared.component.TranslitConverter;

@Component
public class SimpleTranslitConverter implements TranslitConverter {

    private static final Map<Character, String> CYRILLIC_MAP = Map.ofEntries(
            Map.entry('а', "a"),
            Map.entry('б', "b"),
            Map.entry('в', "v"),
            Map.entry('г', "h"),
            Map.entry('ґ', "g"),
            Map.entry('д', "d"),
            Map.entry('е', "e"),
            Map.entry('ж', "zh"),
            Map.entry('з', "z"),
            Map.entry('и', "y"),
            Map.entry('і', "i"),
            Map.entry('к', "k"),
            Map.entry('л', "l"),
            Map.entry('м', "m"),
            Map.entry('н', "n"),
            Map.entry('о', "o"),
            Map.entry('п', "p"),
            Map.entry('р', "r"),
            Map.entry('с', "s"),
            Map.entry('т', "t"),
            Map.entry('у', "u"),
            Map.entry('ф', "f"),
            Map.entry('х', "kh"),
            Map.entry('ц', "ts"),
            Map.entry('ч', "ch"),
            Map.entry('ш', "sh"),
            Map.entry('щ', "shch"),
            Map.entry('ё', "yo"),
            Map.entry('ы', "y"),
            Map.entry('э', "e"),
            Map.entry('ъ', ""),
            Map.entry('ь', "")
    );
    private static final Map<Character, String> IOTATED_START_MAP = Map.ofEntries(
            Map.entry('є', "ye"),
            Map.entry('ї', "yi"),
            Map.entry('й', "y"),
            Map.entry('ю', "yu"),
            Map.entry('я', "ya")
    );
    private static final Map<Character, String> IOTATED_CONTINUATION_MAP = Map.ofEntries(
            Map.entry('є', "ie"),
            Map.entry('ї', "i"),
            Map.entry('й', "i"),
            Map.entry('ю', "iu"),
            Map.entry('я', "ia")
    );

    @Override
    public String translit(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch <= 0x7F) {
                result.append(ch);
                continue;
            }
            char lower = Character.toLowerCase(ch);
            String mapped = resolveIotated(lower, isWordStart(text, i));
            if (mapped == null) {
                mapped = CYRILLIC_MAP.get(lower);
            }
            if (mapped == null) {
                continue;
            }
            if (Character.isUpperCase(ch) && !mapped.isEmpty()) {
                result.append(Character.toUpperCase(mapped.charAt(0)));
                result.append(mapped.substring(1));
            } else {
                result.append(mapped);
            }
        }
        return result.toString();
    }

    private boolean isWordStart(String text, int index) {
        if (index == 0) {
            return true;
        }
        char prev = text.charAt(index - 1);
        return !Character.isLetter(prev);
    }

    private String resolveIotated(char ch, boolean wordStart) {
        return wordStart ? IOTATED_START_MAP.get(ch) : IOTATED_CONTINUATION_MAP.get(ch);
    }
}
