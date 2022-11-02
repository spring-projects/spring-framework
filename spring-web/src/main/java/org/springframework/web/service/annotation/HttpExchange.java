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

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.Mapping;

/**
 * Annotation to declare a method on an HTTP service interface as an HTTP
 * endpoint. The endpoint details are defined statically through attributes of
 * the annotation, as well as through the input method argument types.
 *
 * <p>Supported at the type level to express common attributes, to be inherited
 * by all methods, such as a base URL path.
 *
 * <p>At the method level, it's more common to use one of the following HTTP method
 * specific, shortcut annotations, each of which is itself <em>meta-annotated</em>
 * with {@code HttpExchange}:
 *
 * <ul>
 * <li>{@link GetExchange}
 * <li>{@link PostExchange}
 * <li>{@link PutExchange}
 * <li>{@link PatchExchange}
 * <li>{@link DeleteExchange}
 * </ul>
 *
 * <p>Supported method arguments:
 * <table border="1">
 * <tr>
 * <th>Method Argument</th>
 * <th>Description</th>
 * <th>Resolver</th>
 * </tr>
 * <tr>
 * <td>{@link java.net.URI URI}</td>
 * <td>Dynamically set the URL for the request, overriding the annotation's
 * {@link #url()} attribute</td>
 * <td>{@link org.springframework.web.service.invoker.UrlArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.http.HttpMethod HttpMethod}</td>
 * <td>Dynamically set the HTTP method for the request, overriding the annotation's
 * {@link #method()} attribute</td>
 * <td>{@link org.springframework.web.service.invoker.HttpMethodArgumentResolver
 * HttpMethodArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.web.bind.annotation.RequestHeader @RequestHeader}</td>
 * <td>Add a request header</td>
 * <td>{@link org.springframework.web.service.invoker.RequestHeaderArgumentResolver
 * RequestHeaderArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.web.bind.annotation.PathVariable @PathVariable}</td>
 * <td>Add a path variable for the URI template</td>
 * <td>{@link org.springframework.web.service.invoker.PathVariableArgumentResolver
 * PathVariableArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.web.bind.annotation.RequestBody @RequestBody}</td>
 * <td>Set the body of the request</td>
 * <td>{@link org.springframework.web.service.invoker.RequestBodyArgumentResolver
 * RequestBodyArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.web.bind.annotation.RequestParam @RequestParam}</td>
 * <td>Add a request parameter, either form data if {@code "Content-Type"} is
 * {@code "application/x-www-form-urlencoded"} or query params otherwise</td>
 * <td>{@link org.springframework.web.service.invoker.RequestParamArgumentResolver
 * RequestParamArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.web.bind.annotation.RequestPart @RequestPart}</td>
 * <td>Add a request part, which may be a String (form field),
 * {@link org.springframework.core.io.Resource} (file part), Object (entity to be
 * encoded, e.g. as JSON), {@link HttpEntity} (part content and headers), a
 * {@link org.springframework.http.codec.multipart.Part}, or a
 * {@link org.reactivestreams.Publisher} of any of the above.
 * (</td>
 * <td>{@link org.springframework.web.service.invoker.RequestPartArgumentResolver
 * RequestPartArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.web.bind.annotation.CookieValue @CookieValue}</td>
 * <td>Add a cookie</td>
 * <td>{@link org.springframework.web.service.invoker.CookieValueArgumentResolver
 * CookieValueArgumentResolver}</td>
 * </tr>
 * </table>
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
@Reflective(HttpExchangeReflectiveProcessor.class)
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
