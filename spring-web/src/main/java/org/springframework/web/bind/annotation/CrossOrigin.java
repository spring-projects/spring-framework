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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the annotated method as permitting cross origin requests.
 * By default, all origins and headers are permitted.
 *
 * @since 4.2
 * @author Russell Allen
 * @author Sebastien Deleuze
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CrossOrigin {

	/**
	 * List of allowed origins. {@code "*"} means that all origins are allowed. These values
	 * are placed in the {@code Access-Control-Allow-Origin } header of both the pre-flight
	 * and actual responses. Default value is <b>"*"</b>.
	 */
	String[] origin() default {"*"};

	/**
	 * Indicates which request headers can be used during the actual request. {@code "*"} means
	 * that all headers asked by the client are allowed. This property controls the value of
	 * pre-flight response's {@code Access-Control-Allow-Headers} header. Default value is
	 * <b>"*"</b>.
	 */
	String[] allowedHeaders() default {"*"};

	/**
	 * List of response headers that the user-agent will allow the client to access. This property
	 * controls the value of actual response's {@code Access-Control-Expose-Headers} header.
	 */
	String[] exposedHeaders() default {};

	/**
	 * The HTTP request methods to allow: GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE.
	 * Methods specified here overrides {@code RequestMapping} ones.
	 */
	RequestMethod[] method() default {};

	/**
	 * Set to {@code "true"} if the the browser should include any cookies associated to the domain
	 * of the request being annotated, or "false" if it should not. Empty string "" means undefined.
	 * If true, the pre-flight response will include the header
	 * {@code Access-Control-Allow-Credentials=true}. Default value is <b>"true"</b>.
	 */
	String allowCredentials() default "true";

	/**
	 * Controls the cache duration for pre-flight responses.  Setting this to a reasonable
	 * value can reduce the number of pre-flight request/response interaction required by
	 * the browser. This property controls the value of the {@code Access-Control-Max-Age header}
	 * in the pre-flight response. Value set to -1 means undefined. Default value is
	 * <b>1800</b> seconds, or 30 minutes.
	 */
	long maxAge() default 1800;

}
