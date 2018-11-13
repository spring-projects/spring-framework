/*
 * Copyright 2002-2018 the original author or authors.
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
 * message readers and writers. This is used as follows:
 * <ul>
 * <li>Use {@link ClientCodecConfigurer#create()} or
 * {@link ServerCodecConfigurer#create()} to create an instance.
 * <li>Use {@link #defaultCodecs()} to customize HTTP message readers or writers
 * registered by default.
 * <li>Use {@link #customCodecs()} to add custom HTTP message readers or writers.
 * <li>Use {@link #getReaders()} and {@link #getWriters()} to obtain the list of
 * configured HTTP message readers and writers.
 * </ul>
 *
 * <p>HTTP message readers and writers are divided into 3 categories that are
 * ordered as follows:
 * <ol>
 * <li>Typed readers and writers that support specific types, e.g. byte[], String.
 * <li>Object readers and writers, e.g. JSON, XML.
 * <li>Catch-all readers or writers, e.g. String with any media type.
 * </ol>
 *
 * <p>Typed and object readers are further sub-divided and ordered as follows:
 * <ol>
 * <li>Default HTTP reader and writer registrations.
 * <li>Custom readers and writers.
 * </ol>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface CodecConfigurer {

	/**
	 * Provides a way to customize or replace HTTP message readers and writers
	 * registered by default.
	 * @see #registerDefaults(boolean)
	 */
	DefaultCodecs defaultCodecs();

	/**
	 * Register custom HTTP message readers or writers in addition to the ones
	 * registered by default.
	 */
	CustomCodecs customCodecs();

	/**
	 * Provides a way to completely turn off registration of default HTTP message
	 * readers and writers, and instead rely only on the ones provided via
	 * {@link #customCodecs()}.
	 * <p>By default this is set to {@code "true"} in which case default
	 * registrations are made; setting this to {@code false} disables default
	 * registrations.
	 */
	void registerDefaults(boolean registerDefaults);


	/**
	 * Obtain the configured HTTP message readers.
	 */
	List<HttpMessageReader<?>> getReaders();

	/**
	 * Obtain the configured HTTP message writers.
	 */
	List<HttpMessageWriter<?>> getWriters();


	/**
	 * Customize or replace the HTTP message readers and writers registered by
	 * default. The options are further extended by
	 * {@link ClientCodecConfigurer.ClientDefaultCodecs ClientDefaultCodecs} and
	 * {@link ServerCodecConfigurer.ServerDefaultCodecs ServerDefaultCodecs}.
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

		/**
		 * Override the default Protobuf {@code Decoder}.
		 * @param decoder the decoder instance to use
		 * @since 5.1
		 * @see org.springframework.http.codec.protobuf.ProtobufDecoder
		 */
		void protobufDecoder(Decoder<?> decoder);

		/**
		 * Override the default Protobuf {@code Encoder}.
		 * @param encoder the encoder instance to use
		 * @since 5.1
		 * @see org.springframework.http.codec.protobuf.ProtobufEncoder
		 * @see org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter
		 */
		void protobufEncoder(Encoder<?> encoder);

		/**
		 * Override the default JAXB2 {@code Decoder}.
		 * @param decoder the decoder instance to use
		 * @since 5.1.3
		 * @see org.springframework.http.codec.xml.Jaxb2XmlDecoder
		 */
		void jaxb2Decoder(Decoder<?> decoder);

		/**
		 * Override the default JABX2 {@code Encoder}.
		 * @param encoder the encoder instance to use
		 * @since 5.1.3
		 * @see org.springframework.http.codec.xml.Jaxb2XmlEncoder
		 */
		void jaxb2Encoder(Encoder<?> encoder);

		/**
		 * Whether to log form data at DEBUG level, and headers at TRACE level.
		 * Both may contain sensitive information.
		 * <p>By default set to {@code false} so that request details are not shown.
		 * @param enable whether to enable or not
		 * @since 5.1
		 */
		void enableLoggingRequestDetails(boolean enable);
	}


	/**
	 * Registry for custom HTTP message readers and writers.
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
