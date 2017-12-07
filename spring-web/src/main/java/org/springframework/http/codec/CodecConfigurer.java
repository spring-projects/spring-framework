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

/**
 * Defines a common interface for configuring either client or server HTTP
 * message readers and writers. To obtain an instance use either
 * {@link ClientCodecConfigurer#create()} or
 * {@link ServerCodecConfigurer#create()}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface CodecConfigurer {

	/**
	 * Configure or customize the default HTTP message readers and writers.
	 */
	DefaultCodecs defaultCodecs();

	/**
	 * Whether to register default HTTP message readers and writers.
	 * <p>By default this is set to {@code "true"}; setting this to {@code false}
	 * disables default HTTP message reader and writer registrations.
	 */
	void registerDefaults(boolean registerDefaults);

	/**
	 * Register custom HTTP message readers or writers to use in addition to
	 * the ones registered by default.
	 */
	CustomCodecs customCodecs();

	/**
	 * Obtain the configured HTTP message readers.
	 */
	List<HttpMessageReader<?>> getReaders();

	/**
	 * Obtain the configured HTTP message writers.
	 */
	List<HttpMessageWriter<?>> getWriters();


	/**
	 * Assists with customizing the default HTTP message readers and writers.
	 * @see ClientCodecConfigurer.ClientDefaultCodecs
	 * @see ServerCodecConfigurer.ServerDefaultCodecs
	 */
	interface DefaultCodecs {

		/**
		 * Override the default Jackson JSON {@code Decoder}.
		 * @param decoder the decoder instance to use
		 * @see org.springframework.http.codec.json.Jackson2JsonDecoder
		 */
		void jackson2JsonDecoder(Decoder<?> decoder);

		/**
		 * Override the default Jackson JSON {@code Encoder}.
		 * @param encoder the encoder instance to use
		 * @see org.springframework.http.codec.json.Jackson2JsonEncoder
		 */
		void jackson2JsonEncoder(Encoder<?> encoder);
	}


	/**
	 * Registry and container for custom HTTP message readers and writers.
	 */
	interface CustomCodecs {

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
		 * Add a custom {@link HttpMessageWriter}. For writers of type
		 * {@link EncoderHttpMessageWriter} consider using the shortcut
		 * {@link #encoder(Encoder)} instead.
		 * @param writer the writer to add
		 */
		void writer(HttpMessageWriter<?> writer);
	}

}
