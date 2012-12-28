/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which indicates that a method parameter should be bound to an HTTP cookie.
 * Supported for annotated handler methods in Servlet and Portlet environments.
 *
 * <p>The method parameter may be declared as type {@link javax.servlet.http.Cookie}
 * or as cookie value type (String, int, etc).
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see RequestMapping
 * @see RequestParam
 * @see RequestHeader
 * @see org.springframework.web.bind.annotation.RequestMapping
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
 * @see org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter
 * @see org.springframework.web.portlet.mvc.annotation.AnnotationMethodHandlerAdapter
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CookieValue {

	/**
	 * The name of the cookie to bind to.
	 */
	String value() default "";

	/**
	 * Whether the header is required.
	 * <p>Default is {@code true}, leading to an exception being thrown
	 * in case the header is missing in the request. Switch this to
	 * {@code false} if you prefer a {@code null} in case of the
	 * missing header.
	 * <p>Alternatively, provide a {@link #defaultValue() defaultValue},
	 * which implicitly sets this flag to {@code false}.
	 */
	boolean required() default true;

	/**
	 * The default value to use as a fallback. Supplying a default value implicitly
	 * sets {@link #required()} to false.
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
