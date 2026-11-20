/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.client;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;

/**
 * Provides strategies for use in an {@link ExchangeFunction}.
 *
 * <p>To create an instance, see the static methods {@link #withDefaults()},
 * {@link #builder()}, and {@link #empty()}.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ExchangeStrategies {

	/**
	 * Return {@link HttpMessageReader HttpMessageReaders} to read and decode the response body with.
	 * @return the message readers
	 */
	List<HttpMessageReader<?>> messageReaders();

	/**
	 * Return {@link HttpMessageWriter HttpMessageWriters} to write and encode the request body with.
	 * @return the message writers
	 */
	List<HttpMessageWriter<?>> messageWriters();

	/**
	 * Return a builder to create a new {@link ExchangeStrategies} instance
	 * replicated from the current instance.
	 * @since 5.1.12
	 */
	default Builder mutate() {
		throw new UnsupportedOperationException();
	}


	// Static builder methods

	/**
	 * Return an {@code ExchangeStrategies} instance with default configuration
	 * provided by {@link ClientCodecConfigurer}.
	 */
	static ExchangeStrategies withDefaults() {
		return DefaultExchangeStrategiesBuilder.DEFAULT_EXCHANGE_STRATEGIES;
	}

	/**
	 * Return a builder pre-configured with default configuration to start.
	 * This is the same as {@link #withDefaults()} but returns a mutable builder
	 * for further customizations.
	 */
	static Builder builder() {
		DefaultExchangeStrategiesBuilder builder = new DefaultExchangeStrategiesBuilder();
		builder.defaultConfiguration();
		return builder;
	}

	/**
	 * Return a builder with empty configuration to start.
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
