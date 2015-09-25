/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.portlet.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.Mapping;

/**
 * Annotation for mapping Portlet render requests onto handler methods.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.web.bind.annotation.RequestMapping
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface RenderMapping {

	/**
	 * Alias for {@link #windowState}.
	 */
	@AliasFor("windowState")
	String value() default "";

	/**
	 * The window state that the annotated render method applies for.
	 * <p>If not specified, the render method will be invoked for any
	 * window state within its general mapping.
	 * <p>Standard Portlet specification values are supported: {@code "NORMAL"},
	 * {@code "MAXIMIZED"}, {@code "MINIMIZED"}.
	 * <p>Custom window states can be used as well, as supported by the portal.
	 * @since 4.2
	 * @see #value
	 * @see javax.portlet.PortletRequest#getWindowState()
	 */
	@AliasFor("value")
	String windowState() default "";

	/**
	 * The parameters of the mapped request, narrowing the primary mapping.
	 * <p>Same format for any environment: a sequence of {@code "myParam=myValue"}
	 * style expressions, with a request only mapped if each such parameter is found
	 * to have the given value. {@code "myParam"} style expressions are also supported,
	 * with such parameters having to be present in the request (allowed to have
	 * any value). Finally, {@code "!myParam"} style expressions indicate that the
	 * specified parameter is <i>not</i> supposed to be present in the request.
	 * @see org.springframework.web.bind.annotation.RequestMapping#params()
	 */
	String[] params() default {};

}
