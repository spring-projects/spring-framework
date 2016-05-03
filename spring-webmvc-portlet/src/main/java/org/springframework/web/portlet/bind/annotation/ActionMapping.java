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
 * Annotation for mapping Portlet action requests onto handler methods.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see org.springframework.web.bind.annotation.RequestMapping
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface ActionMapping {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * The name of the action, according to the Portlet 2.0
	 * {@code javax.portlet.action} parameter.
	 * <p>If not specified, the annotated method will be used as a default
	 * handler: i.e. for action requests where no specific action mapping
	 * was found.
	 * <p>Note that all such annotated action methods only apply within the
	 * {@code @RequestMapping} constraints of the containing handler class.
	 * @since 4.2
	 * @see javax.portlet.ActionRequest#ACTION_NAME
	 * @see #value
	 */
	@AliasFor("value")
	String name() default "";

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
