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
 * Defines the strategies to be used for processing {@link HandlerFunction}s. An instance of
 * this class is immutable; instances are typically created through the mutable {@link Builder}:
 * either through {@link #builder()} to set up default strategies, or {@link #empty()} to start from
 * scratch. Alternatively, {@code StrategiesSupplier} instances can be created through
 * {@link #of(Supplier, Supplier, Supplier)}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 * @see RouterFunctions#toHttpHandler(RouterFunction, StrategiesSupplier)
 * @see RouterFunctions#toHandlerMapping(RouterFunction, StrategiesSupplier)
 */
public interface StrategiesSupplier {

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
	 * Return a new {@code StrategiesSupplier} with default initialization.
	 * @return the new {@code StrategiesSupplier}
	 */
	static StrategiesSupplier withDefaults() {
		return builder().build();
	}

	/**
	 * Return a new {@code StrategiesSupplier} based on the given
	 * {@linkplain ApplicationContext application context}.
	 * The returned supplier will search for all {@link HttpMessageReader}, {@link HttpMessageWriter},
	 * and {@link ViewResolver} instances in the given application context and return them for
	 * {@link #messageReaders()}, {@link #messageWriters()}, and {@link #viewResolvers()}
	 * respectively.
	 * @param applicationContext the application context to base the strategies on
	 * @return the new {@code StrategiesSupplier}
	 */
	static StrategiesSupplier of(ApplicationContext applicationContext) {
		return builder(applicationContext).build();
	}

	/**
	 * Return a new {@code StrategiesSupplier} described by the given supplier functions.
	 * All provided supplier function parameters can be {@code null} to indicate an empty
	 * stream is to be returned.
	 * @param messageReaders the supplier function for {@link HttpMessageReader} instances (can be {@code null})
	 * @param messageWriters the supplier function for {@link HttpMessageWriter} instances (can be {@code null})
	 * @param viewResolvers the supplier function for {@link ViewResolver} instances (can be {@code null})
	 * @return the new {@code StrategiesSupplier}
	 */
	static StrategiesSupplier of(Supplier<Stream<HttpMessageReader<?>>> messageReaders,
			Supplier<Stream<HttpMessageWriter<?>>> messageWriters,
			Supplier<Stream<ViewResolver>> viewResolvers) {

		return new StrategiesSupplier() {
			@Override
			public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
				return checkForNull(messageReaders);
			}
			@Override
			public Supplier<Stream<HttpMessageWriter<?>>> messageWriters() {
				return checkForNull(messageWriters);
			}
			@Override
			public Supplier<Stream<ViewResolver>> viewResolvers() {
				return checkForNull(viewResolvers);
			}
			private <T> Supplier<Stream<T>> checkForNull(Supplier<Stream<T>> supplier) {
				return supplier != null ? supplier : Stream::empty;
			}
		};
	}


	// Builder methods

	/**
	 * Return a mutable builder for a {@code StrategiesSupplier} with default initialization.
	 * @return the builder
	 */
	static Builder builder() {
		DefaultStrategiesSupplierBuilder builder = new DefaultStrategiesSupplierBuilder();
		builder.defaultConfiguration();
		return builder;
	}

	/**
	 * Return a mutable builder based on the given {@linkplain ApplicationContext application context}.
	 * The returned builder will search for all {@link HttpMessageReader}, {@link HttpMessageWriter},
	 * and {@link ViewResolver} instances in the given application context and return them for
	 * {@link #messageReaders()}, {@link #messageWriters()}, and {@link #viewResolvers()}
	 * respectively.
	 * @param applicationContext the application context to base the strategies on
	 * @return the builder
	 */
	static Builder builder(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		DefaultStrategiesSupplierBuilder builder = new DefaultStrategiesSupplierBuilder();
		builder.applicationContext(applicationContext);
		return builder;
	}

	/**
	 * Return a mutable, empty builder for a {@code StrategiesSupplier}.
	 * @return the builder
	 */
	static Builder empty() {
		return new DefaultStrategiesSupplierBuilder();
	}


	/**
	 * A mutable builder for a {@link StrategiesSupplier}.
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
		 * Builds the {@link StrategiesSupplier}.
		 * @return the built strategies
		 */
		StrategiesSupplier build();
	}

}
