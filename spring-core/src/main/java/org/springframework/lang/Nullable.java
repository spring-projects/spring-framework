package org.springframework.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.meta.TypeQualifierDefault;

/**
 * Leverage JSR 305 meta-annotations to define the annotated element could be null
 * under some circumstances.
 *
 * Should be used at parameters and return values level in association with
 * {@link NonNullApi} package level annotations.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see javax.annotation.Nullable
 */
@Documented
@javax.annotation.Nullable
@Target({ElementType.METHOD, ElementType.PARAMETER})
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {
}
