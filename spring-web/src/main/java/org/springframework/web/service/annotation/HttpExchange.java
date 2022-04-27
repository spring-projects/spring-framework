/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.service.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.Mapping;


/**
 * Annotation that declares an HTTP service method as an HTTP endpoint defined
 * through attributes of the annotation and method argument values.
 *
 * <p>The annotation may only be used at the type level for example to specify
 * a base URL path. At the method level, use one of the HTTP method specific,
 * shortcut annotations, each of which is <em>meta-annotated</em> with
 * {@link HttpExchange}:
 * <ul>
 * <li>{@link GetExchange}
 * <li>{@link PostExchange}
 * <li>{@link PutExchange}
 * <li>{@link PatchExchange}
 * <li>{@link DeleteExchange}
 * <li>{@link OptionsExchange}
 * <li>{@link HeadExchange}
 * </ul>
 *
 * <p>Supported method arguments:
 * <table>
 * <tr>
 * <th>Method Argument</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.http.HttpMethod}</td>
 * <td>Set the HTTP method for the request, overriding the annotation
 * {@link #method()} attribute value</td>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.web.bind.annotation.PathVariable @PathVariable}</td>
 * <td>Provide a path variable to expand the URI template with. This may be an
 * individual value or a Map of values.</td>
 * </tr>
 * </table>
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface HttpExchange {

	/**
	 * This is an alias for {@link #url}.
	 */
	@AliasFor("url")
	String value() default "";

	/**
	 * The URL for the request, either a full URL or a path only that is relative
	 * to a URL declared in a type-level {@code @HttpExchange}, and/or a globally
	 * configured base URL.
	 * <p>By default, this is empty.
	 */
	@AliasFor("value")
	String url() default "";

	/**
	 * The HTTP method to use.
	 * <p>Supported at the type level as well as at the method level.
	 * When used at the type level, all method-level mappings inherit this value.
	 * <p>By default, this is empty.
	 */
	String method() default "";

	/**
	 * The media type for the {@code "Content-Type"} header.
	 * <p>Supported at the type level as well as at the method level, in which
	 * case the method-level values override type-level values.
	 * <p>By default, this is empty.
	 */
	String contentType() default "";

	/**
	 * The media types for the {@code "Accept"} header.
	 * <p>Supported at the type level as well as at the method level, in which
	 * case the method-level values override type-level values.
	 * <p>By default, this is empty.
	 */
	String[] accept() default {};

}
