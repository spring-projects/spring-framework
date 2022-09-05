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

package org.springframework.messaging.rsocket.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declare a method on an RSocket service interface as an RSocket
 * endpoint. The endpoint route is defined statically through the annotation
 * attributes, and through the input method argument types.
 *
 * <p>Supported at the type level to express common attributes, to be inherited
 * by all methods, such as a base route.
 *
 * <p>Supported method arguments:
 * <table border="1">
 * <tr>
 * <th>Method Argument</th>
 * <th>Description</th>
 * <th>Resolver</th>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.messaging.handler.annotation.DestinationVariable @DestinationVariable}</td>
 * <td>Add a route variable to expand into the route</td>
 * <td>{@link DestinationVariableArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.messaging.handler.annotation.Payload @Payload}</td>
 * <td>Set the input payload(s) for the request</td>
 * <td>{@link PayloadArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link Object} argument followed by {@link org.springframework.util.MimeType} argument</td>
 * <td>Add a metadata value</td>
 * <td>{@link MetadataArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.util.MimeType} argument preceded by {@link Object} argument</td>
 * <td>Specify the mime type for the preceding metadata value</td>
 * <td>{@link MetadataArgumentResolver}</td>
 * </tr>
 * </table>
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RSocketExchange {

	/**
	 * Destination-based mapping expressed by this annotation. This is either
	 * {@link org.springframework.util.AntPathMatcher AntPathMatcher} or
	 * {@link org.springframework.web.util.pattern.PathPattern PathPattern}
	 * based pattern, depending on which is configured, matched to the route of
	 * the stream request.
	 */
	String value() default "";

}
