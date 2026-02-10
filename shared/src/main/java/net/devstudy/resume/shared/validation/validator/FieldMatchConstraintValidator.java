package net.devstudy.resume.shared.validation.validator;

import java.beans.PropertyDescriptor;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.devstudy.resume.shared.validation.annotation.FieldMatch;
import org.springframework.beans.BeanWrapperImpl;

public class FieldMatchConstraintValidator implements ConstraintValidator<FieldMatch, Object> {
    private String first;
    private String second;

    @Override
    public void initialize(FieldMatch constraintAnnotation) {
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

        boolean valid = (firstVal == null && secondVal == null)
                || (firstVal != null && firstVal.equals(secondVal));
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
}
