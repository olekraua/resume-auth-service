package net.devstudy.resume.shared.validation.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import net.devstudy.resume.shared.validation.validator.PasswordsMatchValidator;

/**
 * Валідація збігу двох полів пароля (password і confirmPassword).
 */
@Target({ TYPE })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = { PasswordsMatchValidator.class })
public @interface PasswordsMatch {

    String message() default "Паролі мають збігатися";

    String passwordField() default "password";

    String confirmPasswordField() default "confirmPassword";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
