package net.devstudy.resume.profile.api.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marker annotation to group Profile fields that can be updated via reflection-based field copying.
 */
@Target(FIELD)
@Retention(RUNTIME)
@Documented
public @interface ProfileDataFieldGroup {
}
