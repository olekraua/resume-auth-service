package net.devstudy.resume.shared.validation.validator;

import java.beans.PropertyDescriptor;
import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.devstudy.resume.shared.validation.annotation.FirstFieldLessThanSecond;
import org.springframework.beans.BeanWrapperImpl;

public class FirstFieldLessThanSecondConstraintValidator
        implements ConstraintValidator<FirstFieldLessThanSecond, Object> {
    private String first;
    private String second;

    @Override
    public void initialize(FirstFieldLessThanSecond constraintAnnotation) {
        this.first = constraintAnnotation.first();
        this.second = constraintAnnotation.second();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
        Object firstVal = getProperty(wrapper, first);
        Object secondVal = getProperty(wrapper, second);
        if (firstVal == null || secondVal == null) {
            return true;
        }
        boolean valid = compare(firstVal, secondVal);
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(second)
                    .addConstraintViolation();
        }
        return valid;
    }

    private Object getProperty(BeanWrapperImpl wrapper, String property) {
        PropertyDescriptor pd = wrapper.getPropertyDescriptor(property);
        return (pd == null) ? null : wrapper.getPropertyValue(property);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean compare(Object firstVal, Object secondVal) {
        if (firstVal instanceof LocalDate f && secondVal instanceof LocalDate s) {
            return f.isBefore(s) || f.isEqual(s);
        }
        if (firstVal instanceof Comparable && secondVal instanceof Comparable) {
            try {
                return ((Comparable) firstVal).compareTo(secondVal) <= 0;
            } catch (ClassCastException ignored) {
                return true;
            }
        }
        return true;
    }
}
