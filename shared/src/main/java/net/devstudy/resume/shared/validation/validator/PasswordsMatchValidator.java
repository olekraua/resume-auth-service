package net.devstudy.resume.shared.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.devstudy.resume.shared.validation.annotation.PasswordsMatch;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Перевіряє, що два поля пароля збігаються і не порожні.
 */
public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, Object> {

    private String passwordField;
    private String confirmPasswordField;

    @Override
    public void initialize(PasswordsMatch annotation) {
        this.passwordField = annotation.passwordField();
        this.confirmPasswordField = annotation.confirmPasswordField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        BeanWrapperImpl wrapper = new BeanWrapperImpl(value);
        Object passwordObj = wrapper.getPropertyValue(passwordField);
        Object confirmObj = wrapper.getPropertyValue(confirmPasswordField);

        if (!(passwordObj instanceof String) || !(confirmObj instanceof String)) {
            return false;
        }
        String password = ((String) passwordObj).trim();
        String confirm = ((String) confirmObj).trim();

        boolean valid = !password.isEmpty() && password.equals(confirm);
        if (!valid && context != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(confirmPasswordField)
                    .addConstraintViolation();
        }
        return valid;
    }
}
