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

package org.springframework.messaging.rsocket.annotation.support;

import java.util.function.Consumer;

import io.rsocket.RSocketFactory;

import org.springframework.messaging.handler.invocation.reactive.ArgumentResolverConfigurer;
import org.springframework.messaging.handler.invocation.reactive.ReturnValueHandlerConfigurer;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.RouteMatcher;

/**
 * Build and configure a responder on a {@link RSocketFactory.ClientRSocketFactory} in order
 * to handle requests sent by the RSocket server to the client.
 * <p>This can be configured as a responder on a {@link org.springframework.messaging.rsocket.RSocketRequester}
 * being built by passing it as an argument to the
 * {@link org.springframework.messaging.rsocket.RSocketRequester.Builder#rsocketFactory} method.
 *
 * @author Brian Clozel
 * @since 5.2
 * @see org.springframework.messaging.rsocket.RSocketRequester
 */
public interface ClientResponderFactory extends Consumer<RSocketFactory.ClientRSocketFactory> {

	/**
	 * Create a new {@link ClientResponderFactory.Config} for handling requests with annotated handlers.
	 */
	static ClientResponderFactory.Config create() {
		return new DefaultClientResponderFactory();
	}

	/**
	 * Configure the client responder with infrastructure options
	 * to be applied on the resulting {@link RSocketMessageHandler}.
	 */
	interface Config {

		/**
		 * Set the {@link RSocketStrategies} to use for access to encoders,
		 * decoders, and a factory for {@code DataBuffer's}.
		 * @param strategies the codecs strategies to use
		 */
		Config strategies(RSocketStrategies strategies);

		/**
		 * Set the {@link RouteMatcher} to use for matching incoming requests.
		 * <p>If none is set, then the responder will use a default
		 * {@link org.springframework.util.SimpleRouteMatcher} instance backed
		 * by and {@link org.springframework.util.AntPathMatcher}.
		 * @param routeMatcher the route matcher to use with the responder
		 */
		Config routeMatcher(RouteMatcher routeMatcher);

		/**
		 * Set the {@link MetadataExtractor} to use for extracting information
		 * from metadata frames.
		 * @param extractor the metadata extractor to use
		 */
		Config metadataExtractor(MetadataExtractor extractor);

		/**
		 * Set the {@link ReturnValueHandlerConfigurer} for configuring
		 * return value handlers.
		 * @param configurer the configurer to use
		 */
		Config returnValueHandler(ReturnValueHandlerConfigurer configurer);

		/**
		 * Set the {@link ArgumentResolverConfigurer} for configuring
		 * argument resolvers.
		 * @param configurer the configurer to use
		 */
		Config argumentResolver(ArgumentResolverConfigurer configurer);

		/**
		 * Set the annotated handlers in charge of processing the incoming RSocket requests.
		 * @param handlers the annotated handlers
		 */
		ClientResponderFactory handlers(Object... handlers);

	}

}
