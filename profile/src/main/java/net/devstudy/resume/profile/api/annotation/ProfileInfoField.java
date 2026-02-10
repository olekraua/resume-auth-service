package net.devstudy.resume.profile.api.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marker annotation that identifies Profile fields that should be included into the search index (Elasticsearch).
 */
@Target(FIELD)
@Retention(RUNTIME)
@Documented
public @interface ProfileInfoField {
}
