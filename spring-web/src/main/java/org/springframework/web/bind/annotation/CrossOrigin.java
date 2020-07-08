/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Annotation for permitting cross-origin requests on specific handler classes
 * and/or handler methods. Processed if an appropriate {@code HandlerMapping}
 * is configured.
 *
 * <p>Both Spring Web MVC and Spring WebFlux support this annotation through the
 * {@code RequestMappingHandlerMapping} in their respective modules. The values
 * from each type and method level pair of annotations are added to a
 * {@link CorsConfiguration} and then default values are applied via
 * {@link CorsConfiguration#applyPermitDefaultValues()}.
 *
 * <p>The rules for combining global and local configuration are generally
 * additive -- e.g. all global and all local origins. For those attributes
 * where only a single value can be accepted such as {@code allowCredentials}
 * and {@code maxAge}, the local overrides the global value.
 * See {@link CorsConfiguration#combine(CorsConfiguration)} for more details.
 *
 * @author Russell Allen
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Ruslan Akhundov
 * @since 4.2
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CrossOrigin {

	/** @deprecated as of Spring 5.0, in favor of {@link CorsConfiguration#applyPermitDefaultValues} */
	@Deprecated
	String[] DEFAULT_ORIGINS = {"*"};

	/** @deprecated as of Spring 5.0, in favor of {@link CorsConfiguration#applyPermitDefaultValues} */
	@Deprecated
	String[] DEFAULT_ALLOWED_HEADERS = {"*"};

	/** @deprecated as of Spring 5.0, in favor of {@link CorsConfiguration#applyPermitDefaultValues} */
	@Deprecated
	boolean DEFAULT_ALLOW_CREDENTIALS = false;

	/** @deprecated as of Spring 5.0, in favor of {@link CorsConfiguration#applyPermitDefaultValues} */
	@Deprecated
	long DEFAULT_MAX_AGE = 1800;


	/**
	 * Alias for {@link #origins}.
	 */
	@AliasFor("origins")
	String[] value() default {};

	/**
	 * A list of origins for which cross-origin requests are allowed. Please,
	 * see {@link CorsConfiguration#setAllowedOrigins(List)} for details.
	 * <p>By default all origins are allowed unless {@code originPatterns} is
	 * also set in which case {@code originPatterns} is used instead.
	 */
	@AliasFor("value")
	String[] origins() default {};

	/**
	 * Alternative to {@link #origins()} that supports origins declared via
	 * wildcard patterns. Please, see
	 * @link CorsConfiguration#setAllowedOriginPatterns(List)} for details.
	 * <p>By default this is not set.
	 * @since 5.3
	 */
	String[] originPatterns() default {};

	/**
	 * The list of request headers that are permitted in actual requests,
	 * possibly {@code "*"}  to allow all headers.
	 * <p>Allowed headers are listed in the {@code Access-Control-Allow-Headers}
	 * response header of preflight requests.
	 * <p>A header name is not required to be listed if it is one of:
	 * {@code Cache-Control}, {@code Content-Language}, {@code Expires},
	 * {@code Last-Modified}, or {@code Pragma} as per the CORS spec.
	 * <p>By default all requested headers are allowed.
	 */
	String[] allowedHeaders() default {};

	/**
	 * The List of response headers that the user-agent will allow the client
	 * to access on an actual response, other than "simple" headers, i.e.
	 * {@code Cache-Control}, {@code Content-Language}, {@code Content-Type},
	 * {@code Expires}, {@code Last-Modified}, or {@code Pragma},
	 * <p>Exposed headers are listed in the {@code Access-Control-Expose-Headers}
	 * response header of actual CORS requests.
	 * <p>By default no headers are listed as exposed.
	 */
	String[] exposedHeaders() default {};

	/**
	 * The list of supported HTTP request methods.
	 * <p>By default the supported methods are the same as the ones to which a
	 * controller method is mapped.
	 */
	RequestMethod[] methods() default {};

	/**
	 * Whether the browser should send credentials, such as cookies along with
	 * cross domain requests, to the annotated endpoint. The configured value is
	 * set on the {@code Access-Control-Allow-Credentials} response header of
	 * preflight requests.
	 * <p><strong>NOTE:</strong> Be aware that this option establishes a high
	 * level of trust with the configured domains and also increases the surface
	 * attack of the web application by exposing sensitive user-specific
	 * information such as cookies and CSRF tokens.
	 * <p>By default this is not set in which case the
	 * {@code Access-Control-Allow-Credentials} header is also not set and
	 * credentials are therefore not allowed.
	 */
	String allowCredentials() default "";

	/**
	 * The maximum age (in seconds) of the cache duration for preflight responses.
	 * <p>This property controls the value of the {@code Access-Control-Max-Age}
	 * response header of preflight requests.
	 * <p>Setting this to a reasonable value can reduce the number of preflight
	 * request/response interactions required by the browser.
	 * A negative value means <em>undefined</em>.
	 * <p>By default this is set to {@code 1800} seconds (30 minutes).
	 */
	long maxAge() default -1;

}
