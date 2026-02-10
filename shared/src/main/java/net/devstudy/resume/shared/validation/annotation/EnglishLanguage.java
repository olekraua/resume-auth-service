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
import net.devstudy.resume.shared.validation.validator.EnglishLanguageConstraintValidator;

@Target({ METHOD, FIELD, ANNOTATION_TYPE, PARAMETER })
@Retention(RUNTIME)
@Constraint(validatedBy = EnglishLanguageConstraintValidator.class)
@Documented
public @interface EnglishLanguage {
    String message() default "{EnglishLanguage}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    boolean withPunctuations() default true;
    boolean withNumbers() default true;
}
