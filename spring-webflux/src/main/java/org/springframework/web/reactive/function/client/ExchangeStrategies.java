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

import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;

/**
 * Defines the strategies for invoking {@link ExchangeFunction}s. An instance of
 * this class is immutable; instances are typically created through the mutable {@link Builder}:
 * either through {@link #builder()} to set up default strategies, or {@link #empty()} to start
 * from scratch.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ExchangeStrategies {

	// Instance methods

	/**
	 * Return the {@link HttpMessageReader}s to be used for request body conversion.
	 * @return the stream of message readers
	 */
	List<HttpMessageReader<?>> messageReaders();

	/**
	 * Return the {@link HttpMessageWriter}s to be used for response body conversion.
	 * @return the stream of message writers
	 */
	List<HttpMessageWriter<?>> messageWriters();


	// Static methods

	/**
	 * Return a new {@code ExchangeStrategies} with default initialization.
	 * @return the new {@code ExchangeStrategies}
	 */
	static ExchangeStrategies withDefaults() {
		return builder().build();
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
	 * Return a mutable, empty builder for a {@code ExchangeStrategies}.
	 * @return the builder
	 */
	static Builder empty() {
		return new DefaultExchangeStrategiesBuilder();
	}


	/**
	 * A mutable builder for an {@link ExchangeStrategies}.
	 */
	interface Builder {

		/**
		 * Customize the list of client-side HTTP message readers and writers.
		 * @param consumer the consumer to customize the codecs
		 * @return this builder
		 */
		Builder codecs(Consumer<ClientCodecConfigurer> consumer);

		/**
		 * Builds the {@link ExchangeStrategies}.
		 * @return the built strategies
		 */
		ExchangeStrategies build();
	}

}
