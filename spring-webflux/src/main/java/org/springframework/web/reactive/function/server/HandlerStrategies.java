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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
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
		 * Configure the {@code Encoder} to use for Server-Sent Events.
		 * <p>By default the {@link #jackson2Encoder} override is used for SSE.
		 * @param encoder the encoder to use
		 * @return this builder
		 */
		Builder serverSentEventEncoder(Encoder<?> encoder);

		/**
		 * Override the default Jackson {@code Decoder}.
		 * @param decoder the decoder to use
		 * @return this builder
		 */
		Builder jackson2Decoder(Jackson2JsonDecoder decoder);

		/**
		 * Override the default Jackson {@code Encoder} for JSON.
		 * @param encoder the encoder to use
		 * @return this builder
		 */
		Builder jackson2Encoder(Jackson2JsonEncoder encoder);

		/**
		 * Add a custom {@code Decoder} internally wrapped with
		 * {@link DecoderHttpMessageReader}).
		 * @param decoder the decoder to add
		 * @return this builder
		 */
		Builder customDecoder(Decoder<?> decoder);

		/**
		 * Add a custom {@code Encoder}, internally wrapped with
		 * {@link EncoderHttpMessageWriter}.
		 * @param encoder the encoder to add
		 * @return this builder
		 */
		Builder customEncoder(Encoder<?> encoder);

		/**
		 * Add a custom {@link HttpMessageReader}. For readers of type
		 * {@link DecoderHttpMessageReader} consider using the shortcut
		 * {@link #customDecoder(Decoder)} instead.
		 * @param reader the reader to add
		 * @return this builder
		 */
		Builder customMessageReader(HttpMessageReader<?> reader);

		/**
		 * Add a custom {@link HttpMessageWriter}. For readers of type
		 * {@link EncoderHttpMessageWriter} consider using the shortcut
		 * {@link #customEncoder(Encoder)} instead.
		 * @param writer the writer to add
		 * @return this builder
		 */
		Builder customMessageWriter(HttpMessageWriter<?> writer);

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
