/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that indicates a method parameter should be bound to a template variable
 * in a destination template string. Supported on message handling methods such as
 * {@link MessageMapping @MessageMapping}.
 *
 * <p>A {@code @DestinationVariable} template variable is always required.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see org.springframework.messaging.handler.annotation.MessageMapping
 * @see org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DestinationVariable {

	/**
	 * The name of the destination template variable to bind to.
	 */
	String value() default "";

}
