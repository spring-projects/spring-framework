/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.aspectj;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Signals the current application context to apply dependency injection to
 * non-managed classes that are instantiated outside the Spring bean factory
 * (typically classes annotated with the
 * {@link org.springframework.beans.factory.annotation.Configurable @Configurable}
 * annotation).
 *
 * <p>Similar to functionality found in Spring's
 * {@code <context:spring-configured>} XML element. Often used in conjunction with
 * {@link org.springframework.context.annotation.EnableLoadTimeWeaving @EnableLoadTimeWeaving}.
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.context.annotation.EnableLoadTimeWeaving
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SpringConfiguredConfiguration.class)
public @interface EnableSpringConfigured {

}
