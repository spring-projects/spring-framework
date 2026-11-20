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

import org.springframework.messaging.Message;

/**
 * Annotation that indicates a method's return value should be converted to
 * a {@link Message} if necessary and sent to the specified destination.
 *
 * <p>In a typical request/reply scenario, the incoming {@link Message} may
 * convey the destination to use for the reply. In that case, that destination
 * should take precedence.
 *
* <p>This annotation may be placed class-level in which case it is inherited by
 * methods of the class.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @since 4.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SendTo {

	/**
	 * The destination for a message created from the return value of a method.
	 */
	String[] value() default {};

}
