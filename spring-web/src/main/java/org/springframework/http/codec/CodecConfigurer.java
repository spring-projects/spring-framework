/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.lang.Nullable;

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
	 * Create a copy of this {@link CodecConfigurer}. The returned clone has its
	 * own lists of default and custom codecs and generally can be configured
	 * independently. Keep in mind however that codec instances (if any are
	 * configured) are themselves not cloned.
	 * @since 5.1.12
	 */
	CodecConfigurer clone();


	/**
	 * Customize or replace the HTTP message readers and writers registered by
	 * default. The options are further extended by
	 * {@link ClientCodecConfigurer.ClientDefaultCodecs ClientDefaultCodecs} and
	 * {@link ServerCodecConfigurer.ServerDefaultCodecs ServerDefaultCodecs}.
	 */
	interface DefaultCodecs {

		/**
		 * Override the default Jackson JSON {@code Decoder}.
		 * <p>Note that {@link #maxInMemorySize(int)}, if configured, will be
		 * applied to the given decoder.
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
		 * Override the default Jackson Smile {@code Decoder}.
		 * <p>Note that {@link #maxInMemorySize(int)}, if configured, will be
		 * applied to the given decoder.
		 * @param decoder the decoder instance to use
		 * @see org.springframework.http.codec.json.Jackson2SmileDecoder
		 */
		void jackson2SmileDecoder(Decoder<?> decoder);

		/**
		 * Override the default Jackson Smile {@code Encoder}.
		 * @param encoder the encoder instance to use
		 * @see org.springframework.http.codec.json.Jackson2SmileEncoder
		 */
		void jackson2SmileEncoder(Encoder<?> encoder);

		/**
		 * Override the default Protobuf {@code Decoder}.
		 * <p>Note that {@link #maxInMemorySize(int)}, if configured, will be
		 * applied to the given decoder.
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
		 * <p>Note that {@link #maxInMemorySize(int)}, if configured, will be
		 * applied to the given decoder.
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
		 * Configure a limit on the number of bytes that can be buffered whenever
		 * the input stream needs to be aggregated. This can be a result of
		 * decoding to a single {@code DataBuffer},
		 * {@link java.nio.ByteBuffer ByteBuffer}, {@code byte[]},
		 * {@link org.springframework.core.io.Resource Resource}, {@code String}, etc.
		 * It can also occur when splitting the input stream, e.g. delimited text,
		 * in which case the limit applies to data buffered between delimiters.
		 * <p>By default this is not set, in which case individual codec defaults
		 * apply. All codecs are limited to 256K by default.
		 * @param byteCount the max number of bytes to buffer, or -1 for unlimited
		 * @since 5.1.11
		 */
		void maxInMemorySize(int byteCount);

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
		 * Register a custom codec. This is expected to be one of the following:
		 * <ul>
		 * <li>{@link HttpMessageReader}
		 * <li>{@link HttpMessageWriter}
		 * <li>{@link Encoder} (wrapped internally with {@link EncoderHttpMessageWriter})
		 * <li>{@link Decoder} (wrapped internally with {@link DecoderHttpMessageReader})
		 * </ul>
		 * @param codec the codec to register
		 * @since 5.1.13
		 */
		void register(Object codec);

		/**
		 * Variant of {@link #register(Object)} that also applies the below
		 * properties, if configured, via {@link #defaultCodecs()}:
		 * <ul>
		 * <li>{@link CodecConfigurer.DefaultCodecs#maxInMemorySize(int) maxInMemorySize}
		 * <li>{@link CodecConfigurer.DefaultCodecs#enableLoggingRequestDetails(boolean) enableLoggingRequestDetails}
		 * </ul>
		 * <p>The properties are applied every time {@link #getReaders()} or
		 * {@link #getWriters()} are used to obtain the list of configured
		 * readers or writers.
		 * @param codec the codec to register and apply default config to
		 * @since 5.1.13
		 */
		void registerWithDefaultConfig(Object codec);

		/**
		 * Variant of {@link #register(Object)} that also allows the caller to
		 * apply the properties from {@link DefaultCodecConfig} to the given
		 * codec. If you want to apply all the properties, prefer using
		 * {@link #registerWithDefaultConfig(Object)}.
		 * <p>The consumer is called every time {@link #getReaders()} or
		 * {@link #getWriters()} are used to obtain the list of configured
		 * readers or writers.
		 * @param codec the codec to register
		 * @param configConsumer consumer of the default config
		 * @since 5.1.13
		 */
		void registerWithDefaultConfig(Object codec, Consumer<DefaultCodecConfig> configConsumer);

		/**
		 * Add a custom {@code Decoder} internally wrapped with
		 * {@link DecoderHttpMessageReader}).
		 * @param decoder the decoder to add
		 * @deprecated as of 5.1.13, use {@link #register(Object)} or
		 * {@link #registerWithDefaultConfig(Object)} instead.
		 */
		@Deprecated
		void decoder(Decoder<?> decoder);

		/**
		 * Add a custom {@code Encoder}, internally wrapped with
		 * {@link EncoderHttpMessageWriter}.
		 * @param encoder the encoder to add
		 * @deprecated as of 5.1.13, use {@link #register(Object)} or
		 * {@link #registerWithDefaultConfig(Object)} instead.
		 */
		@Deprecated
		void encoder(Encoder<?> encoder);

		/**
		 * Add a custom {@link HttpMessageReader}. For readers of type
		 * {@link DecoderHttpMessageReader} consider using the shortcut
		 * {@link #decoder(Decoder)} instead.
		 * @param reader the reader to add
		 * @deprecated as of 5.1.13, use {@link #register(Object)} or
		 * {@link #registerWithDefaultConfig(Object)} instead.
		 */
		@Deprecated
		void reader(HttpMessageReader<?> reader);

		/**
		 * Add a custom {@link HttpMessageWriter}. For writers of type
		 * {@link EncoderHttpMessageWriter} consider using the shortcut
		 * {@link #encoder(Encoder)} instead.
		 * @param writer the writer to add
		 * @deprecated as of 5.1.13, use {@link #register(Object)} or
		 * {@link #registerWithDefaultConfig(Object)} instead.
		 */
		@Deprecated
		void writer(HttpMessageWriter<?> writer);

		/**
		 * Register a callback for the {@link DefaultCodecConfig configuration}
		 * applied to default codecs. This allows custom codecs to follow general
		 * guidelines applied to default ones, such as logging details and limiting
		 * the amount of buffered data.
		 * @param codecsConfigConsumer the default codecs configuration callback
		 * @deprecated as of 5.1.13, use {@link #registerWithDefaultConfig(Object)}
		 * or {@link #registerWithDefaultConfig(Object, Consumer)} instead.
		 */
		@Deprecated
		void withDefaultCodecConfig(Consumer<DefaultCodecConfig> codecsConfigConsumer);
	}


	/**
	 * Exposes the values of properties configured through
	 * {@link #defaultCodecs()} that are applied to default codecs.
	 * The main purpose of this interface is to provide access to them so they
	 * can also be applied to custom codecs if needed.
	 * @since 5.1.12
	 * @see CustomCodecs#registerWithDefaultConfig(Object, Consumer)
	 */
	interface DefaultCodecConfig {

		/**
		 * Get the configured limit on the number of bytes that can be buffered whenever
		 * the input stream needs to be aggregated.
		 */
		@Nullable
		Integer maxInMemorySize();

		/**
		 * Whether to log form data at DEBUG level, and headers at TRACE level.
		 * Both may contain sensitive information.
		 */
		@Nullable
		Boolean isEnableLoggingRequestDetails();
	}

}
