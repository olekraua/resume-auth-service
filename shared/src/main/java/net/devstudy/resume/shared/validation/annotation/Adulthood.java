package net.devstudy.resume.shared.validation.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import net.devstudy.resume.shared.validation.validator.AdulthoodConstraintValidator;

/**
 * Валідація, що дата народження вказує на повноліття (за замовчуванням 18 років).
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = { AdulthoodConstraintValidator.class })
public @interface Adulthood {

    String message() default "{Adulthood}";

    int adulthoodAge() default 18;

    Class<? extends Payload>[] payload() default { };

    Class<?>[] groups() default { };
}
