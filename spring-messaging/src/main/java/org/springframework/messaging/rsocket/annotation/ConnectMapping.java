/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.rsocket.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.rsocket.ConnectionSetupPayload;

/**
 * Annotation to map the initial {@link ConnectionSetupPayload} and subsequent
 * metadata pushes onto a handler method.
 *
 * <p>This is a method-level annotation that can be combined with a type-level
 * {@link org.springframework.messaging.handler.annotation.MessageMapping @MessageMapping}
 * for a combined route pattern. It supports the same arguments as
 * {@code @MessageMapping} but the return value must be {@code void}. On a
 * server, handling can be asynchronous (e.g. {@code Mono<Void>}), in which
 * case the connection is accepted if and when the {@code Mono<Void>} completes.
 * On the client side this method is only a callback and does not affect the
 * establishment of the connection.
 *
 * <p><strong>Note:</strong> an {@code @ConnectMapping} method may start
 * requests to the remote through an
 * {@link org.springframework.messaging.rsocket.RSocketRequester RSocketRequester}
 * method argument, but it must do so independent of the handling thread (e.g.
 * via subscribing on a different thread).
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConnectMapping {

	/**
	 * Mappings expressed by this annotation to match to the route from the
	 * metadata of the initial {@link ConnectionSetupPayload} or in
	 * subsequent metadata pushes.
	 * <p>Depending on the configured
	 * {@link org.springframework.util.RouteMatcher RouteMatcher}, the pattern may be
	 * {@link org.springframework.util.AntPathMatcher AntPathMatcher} or
	 * {@link org.springframework.web.util.pattern.PathPattern PathPattern} based.
	 * <p>By default this is an empty array in which case it matches all
	 * {@link ConnectionSetupPayload} and metadata pushes.
	 */
	String[] value() default {};

}
