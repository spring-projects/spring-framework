/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.annotation.Reflective;

/**
 * Container annotation that aggregates several {@link Scheduled} annotations.
 *
 * <p>Can be used natively, declaring several nested {@link Scheduled} annotations.
 * Can also be used in conjunction with Java's support for repeatable annotations,
 * where {@link Scheduled @Scheduled} can simply be declared several times on the
 * same method, implicitly generating this container annotation.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see Scheduled
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective
public @interface Schedules {

	Scheduled[] value();

}
