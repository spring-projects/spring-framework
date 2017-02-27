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

package org.springframework.web.reactive.function.client;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.context.ApplicationContext;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.Assert;

/**
 * Defines the strategies for invoking {@link ExchangeFunction}s. An instance of
 * this class is immutable; instances are typically created through the mutable {@link Builder}:
 * either through {@link #builder()} to set up default strategies, or {@link #empty()} to start
 * from scratch. Alternatively, {@code ExchangeStrategies} instances can be created through
 * {@link #of(Supplier, Supplier)}.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ExchangeStrategies {

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


	// Static methods

	/**
	 * Return a new {@code ExchangeStrategies} with default initialization.
	 * @return the new {@code ExchangeStrategies}
	 */
	static ExchangeStrategies withDefaults() {
		return builder().build();
	}

	/**
	 * Return a new {@code ExchangeStrategies} based on the given
	 * {@linkplain ApplicationContext application context}.
	 * The returned supplier will search for all {@link HttpMessageReader}, and
	 * {@link HttpMessageWriter} instances in the given application context and return
	 * them for {@link #messageReaders()}, and {@link #messageWriters()} respectively.
	 * @param applicationContext the application context to base the strategies on
	 * @return the new {@code ExchangeStrategies}
	 */
	static ExchangeStrategies of(ApplicationContext applicationContext) {
		return builder(applicationContext).build();
	}

	/**
	 * Return a new {@code ExchangeStrategies} described by the given supplier functions.
	 * All provided supplier function parameters can be {@code null} to indicate an empty
	 * stream is to be returned.
	 * @param messageReaders the supplier function for {@link HttpMessageReader} instances
	 * (can be {@code null})
	 * @param messageWriters the supplier function for {@link HttpMessageWriter} instances
	 * (can be {@code null})
	 * @return the new {@code ExchangeStrategies}
	 */
	static ExchangeStrategies of(Supplier<Stream<HttpMessageReader<?>>> messageReaders,
			Supplier<Stream<HttpMessageWriter<?>>> messageWriters) {

		return new ExchangeStrategies() {
			@Override
			public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
				return checkForNull(messageReaders);
			}
			@Override
			public Supplier<Stream<HttpMessageWriter<?>>> messageWriters() {
				return checkForNull(messageWriters);
			}
			private <T> Supplier<Stream<T>> checkForNull(Supplier<Stream<T>> supplier) {
				return supplier != null ? supplier : Stream::empty;
			}
		};
	}


	// Builder methods

	/**
	 * Return a mutable builder for a {@code ExchangeStrategies} with default initialization.
	 * @return the builder
	 */
	static Builder builder() {
		DefaultExchangeStrategiesBuilder builder = new DefaultExchangeStrategiesBuilder();
		builder.defaultConfiguration();
		return builder;
	}

	/**
	 * Return a mutable builder based on the given {@linkplain ApplicationContext application context}.
	 * The returned builder will search for all {@link HttpMessageReader}, and
	 * {@link HttpMessageWriter} instances in the given application context and return them for
	 * {@link #messageReaders()}, and {@link #messageWriters()}.
	 * @param applicationContext the application context to base the strategies on
	 * @return the builder
	 */
	static Builder builder(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		DefaultExchangeStrategiesBuilder builder = new DefaultExchangeStrategiesBuilder();
		builder.applicationContext(applicationContext);
		return builder;
	}

	/**
	 * Return a mutable, empty builder for a {@code ExchangeStrategies}.
	 * @return the builder
	 */
	static Builder empty() {
		return new DefaultExchangeStrategiesBuilder();
	}


	/**
	 * A mutable builder for a {@link ExchangeStrategies}.
	 */
	interface Builder {

		/**
		 * Add the given message reader to this builder.
		 * @param messageReader the message reader to add
		 * @return this builder
		 */
		Builder messageReader(HttpMessageReader<?> messageReader);

		/**
		 * Add the given decoder to this builder. This is a convenient alternative to adding a
		 * {@link org.springframework.http.codec.DecoderHttpMessageReader} that wraps the given decoder.
		 * @param decoder the decoder to add
		 * @return this builder
		 */
		Builder decoder(Decoder<?> decoder);

		/**
		 * Add the given message writer to this builder.
		 * @param messageWriter the message writer to add
		 * @return this builder
		 */
		Builder messageWriter(HttpMessageWriter<?> messageWriter);

		/**
		 * Add the given encoder to this builder. This is a convenient alternative to adding a
		 * {@link org.springframework.http.codec.EncoderHttpMessageWriter} that wraps the given encoder.
		 * @param encoder the encoder to add
		 * @return this builder
		 */
		Builder encoder(Encoder<?> encoder);

		/**
		 * Builds the {@link ExchangeStrategies}.
		 * @return the built strategies
		 */
		ExchangeStrategies build();
	}

}
