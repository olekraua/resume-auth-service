package net.devstudy.resume.shared.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.devstudy.resume.shared.validation.annotation.MinDigitCount;

public class MinDigitCountConstraintValidator implements ConstraintValidator<MinDigitCount, CharSequence> {
    private int minValue;

    @Override
    public void initialize(MinDigitCount constraintAnnotation) {
        minValue = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        if (minValue <= 0) {
            return true;
        }
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                count++;
                if (count >= minValue) {
                    return true;
                }
            }
        }
        return false;
    }
}
