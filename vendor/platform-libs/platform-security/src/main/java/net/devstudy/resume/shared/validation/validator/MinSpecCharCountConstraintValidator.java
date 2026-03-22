package net.devstudy.resume.shared.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.devstudy.resume.shared.validation.annotation.MinSpecCharCount;

public class MinSpecCharCountConstraintValidator implements ConstraintValidator<MinSpecCharCount, CharSequence> {
    private int minValue;
    private String specSymbols;

    @Override
    public void initialize(MinSpecCharCount constraintAnnotation) {
        minValue = constraintAnnotation.value();
        specSymbols = constraintAnnotation.specSymbols();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        if (minValue <= 0) {
            return true;
        }
        if (specSymbols == null || specSymbols.isEmpty()) {
            return false;
        }
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (specSymbols.indexOf(value.charAt(i)) != -1) {
                count++;
                if (count >= minValue) {
                    return true;
                }
            }
        }
        return false;
    }
}
