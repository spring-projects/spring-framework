package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation that aggregates several {@link RequestMapping} annotations.
 *
 * <p>Can be used natively, declaring several nested {@link RequestMapping} annotations.
 * Can also be used in conjunction with Java 8's support for repeatable annotations,
 * where {@link RequestMapping} can simply be declared several times on the same method,
 * implicitly generating this container annotation.
 *
 * @see RequestMapping
 * @since 6.2
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMappings {
	RequestMapping[] value();
}
