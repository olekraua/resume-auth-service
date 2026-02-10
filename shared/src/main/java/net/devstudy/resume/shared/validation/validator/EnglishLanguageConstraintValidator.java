package net.devstudy.resume.shared.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.devstudy.resume.shared.validation.annotation.EnglishLanguage;

public class EnglishLanguageConstraintValidator implements ConstraintValidator<EnglishLanguage, String> {
    private boolean withPunctuations;
    private boolean withNumbers;
    private String pattern;

    @Override
    public void initialize(EnglishLanguage constraintAnnotation) {
        this.withPunctuations = constraintAnnotation.withPunctuations();
        this.withNumbers = constraintAnnotation.withNumbers();
        StringBuilder sb = new StringBuilder("^[-a-zA-Z ");
        if (withPunctuations) {
            sb.append(".,'!()\\[\\]{}:;\"?");
        }
        if (withNumbers) {
            sb.append("0-9");
        }
        sb.append("]+$");
        pattern = sb.toString();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) return true;
        return value.matches(pattern);
    }
}
