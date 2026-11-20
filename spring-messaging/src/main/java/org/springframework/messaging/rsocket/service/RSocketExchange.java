/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.annotation.Reflective;

/**
 * Annotation to declare an RSocket endpoint on an RSocket service interface.
 * Supported for use as an RSocket requester via
 * {@link RSocketServiceProxyFactory}, and as a responder by implementing the
 * interface to handle requests.
 *
 * <p>The endpoint route is determined through the annotation attribute,
 * and through the method arguments.
 *
 * <p>The annotation is supported at the type level to express a common route,
 * to be inherited by all methods.
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
 * <td>{@link Object}, if followed by {@link org.springframework.util.MimeType}</td>
 * <td>Add a metadata value</td>
 * <td>{@link MetadataArgumentResolver}</td>
 * </tr>
 * <tr>
 * <td>{@link org.springframework.util.MimeType}</td>
 * <td>Set the MIME type for the metadata value in the preceding argument</td>
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
@Reflective(RSocketExchangeReflectiveProcessor.class)
public @interface RSocketExchange {

	/**
	 * Destination-based mapping expressed by this annotation. This is either
	 * {@link org.springframework.util.AntPathMatcher AntPathMatcher} or
	 * {@link org.springframework.web.util.pattern.PathPattern PathPattern}
	 * based pattern, depending on which is configured via
	 * {@link org.springframework.messaging.rsocket.RSocketStrategies} in
	 * {@link org.springframework.messaging.rsocket.RSocketRequester}.
	 */
	String value() default "";

}
