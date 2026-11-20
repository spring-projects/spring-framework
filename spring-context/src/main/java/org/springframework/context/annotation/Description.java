/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a textual description to bean definitions derived from
 * {@link org.springframework.stereotype.Component} or {@link Bean}.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see org.springframework.beans.factory.config.BeanDefinition#getDescription()
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Description {

	/**
	 * The textual description to associate with the bean definition.
	 */
	String value();

}
