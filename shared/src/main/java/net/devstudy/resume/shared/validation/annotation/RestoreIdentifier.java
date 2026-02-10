package net.devstudy.resume.shared.validation.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import net.devstudy.resume.shared.validation.validator.RestoreIdentifierConstraintValidator;

@Target({ METHOD, FIELD, ANNOTATION_TYPE, PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = RestoreIdentifierConstraintValidator.class)
public @interface RestoreIdentifier {

    String message() default "{restore.identifier.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
