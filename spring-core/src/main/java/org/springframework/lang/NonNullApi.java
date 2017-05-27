package org.springframework.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

/**
 * Leverage JSR 305 meta-annotations to define that parameters and return values are
 * non-nullable by default.
 *
 * Should be used at package level in association with {@link Nullable} parameters and
 * return values level annotations.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see javax.annotation.Nonnull
 */
@Documented
@Nonnull
@Target(ElementType.PACKAGE)
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NonNullApi {
}
