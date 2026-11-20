/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to bind values from a web request such as request parameters or
 * path variables to fields of a Java object. Supported on constructor parameters
 * of {@link ModelAttribute @ModelAttribute} controller method arguments
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 * @see org.springframework.web.bind.WebDataBinder#construct
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BindParam {

	/**
	 * The lookup name to use for the bind value.
	 */
	String value() default "";

}
