/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.context.ApplicationContext;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.Assert;
import org.springframework.web.reactive.result.view.ViewResolver;

/**
 * Defines the configuration to be used for processing {@link HandlerFunction}s. An instance of
 * this class is immutable; instances are typically created through the mutable {@link Builder}:
 * either through {@link #builder()} to create a default configuration, {@link #empty()} to create
 * an empty configuration.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface Configuration {

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

	// Static methods

	/**
	 * Return a mutable, empty builder for a {@code Configuration}.
	 * @return the builder
	 */
	static Builder empty() {
		return new DefaultConfigurationBuilder();
	}

	/**
	 * Return a mutable builder for a {@code Configuration} with a default initialization.
	 * @return the builder
	 */
	static Builder builder() {
		DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
		builder.defaultConfiguration();
		return builder;
	}

	/**
	 * Return a mutable builder based on the given {@linkplain ApplicationContext application context}.
	 * The returned builder will search for all {@link HttpMessageReader}, {@link HttpMessageWriter},
	 * and {@link ViewResolver} instances in the given application context and return them for
	 * {@link #messageReaders()}, {@link #messageWriters()}, and {@link #viewResolvers()} in the
	 * built configuration respectively.
	 * @param applicationContext the application context to base the configuration on
	 * @return the builder
	 */
	static Builder applicationContext(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "'applicationContext' must not be null");
		DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
		builder.applicationContext(applicationContext);
		return builder;
	}


	/**
	 * A mutable builder for a {@link Configuration}.
	 */
	interface Builder {

		/**
		 * Add the given message reader to this builder.
		 * @param messageReader the message reader to add
		 * @return this builder
		 */
		Builder messageReader(HttpMessageReader<?> messageReader);

		/**
		 * Add the given message writer to this builder.
		 * @param messageWriter the message writer to add
		 * @return this builder
		 */
		Builder messageWriter(HttpMessageWriter<?> messageWriter);

		/**
		 * Add the given view resolver to this builder.
		 * @param viewResolver the view resolver to add
		 * @return this builder
		 */
		Builder viewResolver(ViewResolver viewResolver);

		/**
		 * Builds the {@link Configuration}.
		 * @return the built configuration
		 */
		Configuration build();

	}
}
