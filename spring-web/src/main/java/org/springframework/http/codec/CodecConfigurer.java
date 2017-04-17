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

package org.springframework.http.codec;

import java.util.List;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;

/**
 * Defines the interface for client or server HTTP message reader and writer configurers.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface CodecConfigurer {

	/**
	 * Provide overrides for built-in HTTP message readers or writers.
	 */
	DefaultCodecsConfigurer defaultCodecs();

	/**
	 * Whether to make default HTTP message reader and writer registrations.
	 * <p>By default this is set to {@code "true"}.
	 */
	void registerDefaults(boolean registerDefaults);

	/**
	 * Register a custom encoder or decoder.
	 */
	CustomCodecsConfigurer customCodecs();

	/**
	 * Prepare a list of HTTP message readers.
	 */
	List<HttpMessageReader<?>> getReaders();

	/**
	 * Prepare a list of HTTP message writers.
	 */
	List<HttpMessageWriter<?>> getWriters();


	/**
	 * Registry and container for built-in HTTP message readers and writers.
	 * @see ClientCodecConfigurer.ClientDefaultCodecsConfigurer
	 * @see ServerCodecConfigurer.ServerDefaultCodecsConfigurer
	 */
	interface DefaultCodecsConfigurer {

		/**
		 * Override the default Jackson {@code Decoder}.
		 * @param decoder the decoder to use
		 */
		void jackson2Decoder(Jackson2JsonDecoder decoder);

		/**
		 * Override the default Jackson {@code Encoder} for JSON.
		 * @param encoder the encoder to use
		 */
		void jackson2Encoder(Jackson2JsonEncoder encoder);

	}


	/**
	 * Registry and container for custom HTTP message readers and writers.
	 */
	interface CustomCodecsConfigurer {

		/**
		 * Add a custom {@code Decoder} internally wrapped with
		 * {@link DecoderHttpMessageReader}).
		 * @param decoder the decoder to add
		 */
		void decoder(Decoder<?> decoder);

		/**
		 * Add a custom {@code Encoder}, internally wrapped with
		 * {@link EncoderHttpMessageWriter}.
		 * @param encoder the encoder to add
		 */
		void encoder(Encoder<?> encoder);

		/**
		 * Add a custom {@link HttpMessageReader}. For readers of type
		 * {@link DecoderHttpMessageReader} consider using the shortcut
		 * {@link #decoder(Decoder)} instead.
		 * @param reader the reader to add
		 */
		void reader(HttpMessageReader<?> reader);

		/**
		 * Add a custom {@link HttpMessageWriter}. For readers of type
		 * {@link EncoderHttpMessageWriter} consider using the shortcut
		 * {@link #encoder(Encoder)} instead.
		 * @param writer the writer to add
		 */
		void writer(HttpMessageWriter<?> writer);

	}
}
