package net.devstudy.resume.media.internal.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a method whose image-processing logic needs a temporary file storage.
 * {@link net.devstudy.resume.media.internal.component.impl.UploadImageTempStorage} creates temp files
 * before method execution and removes them afterwards.
 */
@Target({ METHOD })
@Retention(RUNTIME)
@Documented
public @interface EnableUploadImageTempStorage {
}
