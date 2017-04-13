/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

/**
 * Defines the strategies to be used for processing {@link HandlerFunction}s. An instance of
 * this class is immutable; instances are typically created through the mutable {@link Builder}:
 * either through {@link #builder()} to set up default strategies, or {@link #empty()} to start from
 * scratch.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 * @see RouterFunctions#toHttpHandler(RouterFunction, HandlerStrategies)
 * @see RouterFunctions#toHandlerMapping(RouterFunction, HandlerStrategies)
 */
public interface HandlerStrategies {

	// Instance methods

	/**
	 * Supply a {@linkplain Stream stream} of {@link HttpMessageReader}s to be used for request
	 * body conversion.
	 * @return the stream of message readers
	 */
	Supplier<Stream<HttpMessageReader<?>>> messageReaders();

	/**
	 * Supply a {@linkplain Stream stream} of {@link HttpMessageWriter}s to be used for response
	 * body conversion.
	 * @return the stream of message writers
	 */
	Supplier<Stream<HttpMessageWriter<?>>> messageWriters();

	/**
	 * Supply a {@linkplain Stream stream} of {@link ViewResolver}s to be used for view name
	 * resolution.
	 * @return the stream of view resolvers
	 */
	Supplier<Stream<ViewResolver>> viewResolvers();

	/**
	 * Supply a function that resolves the locale of a given {@link ServerRequest}.
	 * @return the locale resolver
	 */
	Supplier<Function<ServerRequest, Optional<Locale>>> localeResolver();


	// Static methods

	/**
	 * Return a new {@code HandlerStrategies} with default initialization.
	 * @return the new {@code HandlerStrategies}
	 */
	static HandlerStrategies withDefaults() {
		return builder().build();
	}

	// Builder methods

	/**
	 * Return a mutable builder for a {@code HandlerStrategies} with default initialization.
	 * @return the builder
	 */
	static Builder builder() {
		DefaultHandlerStrategiesBuilder builder = new DefaultHandlerStrategiesBuilder();
		builder.defaultConfiguration();
		return builder;
	}

	/**
	 * Return a mutable, empty builder for a {@code HandlerStrategies}.
	 * @return the builder
	 */
	static Builder empty() {
		return new DefaultHandlerStrategiesBuilder();
	}


	/**
	 * A mutable builder for a {@link HandlerStrategies}.
	 */
	interface Builder {

		/**
		 * Customize the list of default server-side HTTP message readers and writers.
		 * @param consumer the consumer to customize the default codecs
		 * @return this builder
		 * @see #customCodecs(Consumer)
		 */
		Builder defaultCodecs(Consumer<ServerCodecConfigurer.ServerDefaultCodecsConfigurer> consumer);

		/**
		 * Customize the list of custom server-side HTTP message readers and writers.
		 * @param consumer the consumer to customize the custom codecs
		 * @return this builder
		 * @see #defaultCodecs(Consumer)
		 */
		Builder customCodecs(Consumer<ServerCodecConfigurer.CustomCodecsConfigurer> consumer);

		/**
		 * Add the given view resolver to this builder.
		 * @param viewResolver the view resolver to add
		 * @return this builder
		 */
		Builder viewResolver(ViewResolver viewResolver);

		/**
		 * Set the given function as {@link Locale} resolver for this builder.
		 * @param localeResolver the locale resolver to set
		 * @return this builder
		 */
		Builder localeResolver(Function<ServerRequest, Optional<Locale>> localeResolver);

		/**
		 * Builds the {@link HandlerStrategies}.
		 * @return the built strategies
		 */
		HandlerStrategies build();
	}

}
