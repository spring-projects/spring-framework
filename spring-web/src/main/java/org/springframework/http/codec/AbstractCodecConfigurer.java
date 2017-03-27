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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.DataBufferDecoder;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Base class for client or server codec configurers.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractCodecConfigurer {

	public static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					ServerCodecConfigurer.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
							ServerCodecConfigurer.class.getClassLoader());

	public static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder", ServerCodecConfigurer.class.getClassLoader());


	private final DefaultCodecConfigurer defaultCodecs;

	private final CustomCodecConfigurer customCodecs = new CustomCodecConfigurer();


	/**
	 * Protected constructor with the configurer for default readers and writers.
	 */
	protected AbstractCodecConfigurer(DefaultCodecConfigurer defaultCodecConfigurer) {
		Assert.notNull(defaultCodecConfigurer, "DefaultCodecConfigurer is required.");
		this.defaultCodecs = defaultCodecConfigurer;
	}


	/**
	 * Provide overrides for built-in HTTP message readers or writers.
	 */
	public DefaultCodecConfigurer defaultCodec() {
		return this.defaultCodecs;
	}

	/**
	 * Whether to make default HTTP message reader and writer registrations.
	 * <p>By default this is set to {@code "true"}.
	 */
	public void registerDefaults(boolean registerDefaults) {
		this.defaultCodec().setSuppressed(!registerDefaults);
	}

	/**
	 * Register a custom encoder or decoder.
	 */
	public CustomCodecConfigurer customCodec() {
		return this.customCodecs;
	}


	/**
	 * Prepare a list of HTTP message readers.
	 */
	public List<HttpMessageReader<?>> getReaders() {

		List<HttpMessageReader<?>> result = new ArrayList<>();

		addDefaultTypedReaders(result);
		addCustomTypedReaders(result);

		addDefaultObjectReaders(result);
		addCustomObjectReaders(result);

		defaultCodec().addStringReaderTo(result);

		return result;
	}

	/**
	 * Add built-in, concrete, Java type readers.
	 */
	protected void addDefaultTypedReaders(List<HttpMessageReader<?>> result) {
		defaultCodec().addReaderTo(result, ByteArrayDecoder.class, ByteArrayDecoder::new);
		defaultCodec().addReaderTo(result, ByteBufferDecoder.class, ByteBufferDecoder::new);
		defaultCodec().addReaderTo(result, DataBufferDecoder.class, DataBufferDecoder::new);
		defaultCodec().addReaderTo(result, ResourceDecoder.class, ResourceDecoder::new);
		defaultCodec().addStringReaderTextOnlyTo(result);
	}

	/**
	 * Add custom, concrete, Java type readers.
	 */
	protected void addCustomTypedReaders(List<HttpMessageReader<?>> result) {
		customCodec().addTypedReadersTo(result);
	}

	/**
	 * Add built-in, Object-based readers.
	 */
	protected void addDefaultObjectReaders(List<HttpMessageReader<?>> result) {
		if (jaxb2Present) {
			defaultCodec().addReaderTo(result, Jaxb2XmlDecoder.class, Jaxb2XmlDecoder::new);
		}
		if (jackson2Present) {
			defaultCodec().addReaderTo(result, Jackson2JsonDecoder.class, Jackson2JsonDecoder::new);
		}
	}

	/**
	 * Add custom, Object-based readers.
	 */
	protected void addCustomObjectReaders(List<HttpMessageReader<?>> result) {
		customCodec().addObjectReadersTo(result);
	}


	/**
	 * Prepare a list of HTTP message writers.
	 */
	public List<HttpMessageWriter<?>> getWriters() {

		List<HttpMessageWriter<?>> result = new ArrayList<>();

		addDefaultTypedWriter(result);
		addCustomTypedWriter(result);

		addDefaultObjectWriters(result);
		addCustomObjectWriters(result);

		// String + "*/*"
		defaultCodec().addStringWriterTo(result);

		return result;
	}

	/**
	 * Add built-in, concrete, Java type readers.
	 */
	protected void addDefaultTypedWriter(List<HttpMessageWriter<?>> result) {
		defaultCodec().addWriterTo(result, ByteArrayEncoder.class, ByteArrayEncoder::new);
		defaultCodec().addWriterTo(result, ByteBufferEncoder.class, ByteBufferEncoder::new);
		defaultCodec().addWriterTo(result, DataBufferEncoder.class, DataBufferEncoder::new);
		defaultCodec().addWriterTo(result, ResourceHttpMessageWriter::new);
		defaultCodec().addStringWriterTextPlainOnlyTo(result);
	}

	/**
	 * Add custom, concrete, Java type readers.
	 */
	protected void addCustomTypedWriter(List<HttpMessageWriter<?>> result) {
		customCodec().addTypedWritersTo(result);
	}

	/**
	 * Add built-in, Object-based readers.
	 */
	protected void addDefaultObjectWriters(List<HttpMessageWriter<?>> result) {
		if (jaxb2Present) {
			defaultCodec().addWriterTo(result, Jaxb2XmlEncoder.class, Jaxb2XmlEncoder::new);
		}
		if (jackson2Present) {
			defaultCodec().addWriterTo(result, Jackson2JsonEncoder.class, Jackson2JsonEncoder::new);
		}
	}

	/**
	 * Add custom, Object-based readers.
	 */
	protected void addCustomObjectWriters(List<HttpMessageWriter<?>> result) {
		customCodec().addObjectWritersTo(result);
	}



	/**
	 * A registry and a factory for built-in HTTP message readers and writers.
	 */
	public static class DefaultCodecConfigurer {

		private boolean suppressed = false;

		private final Map<Class<?>, HttpMessageReader<?>> readers = new HashMap<>();

		private final Map<Class<?>, HttpMessageWriter<?>> writers = new HashMap<>();


		/**
		 * Override the default Jackson {@code Decoder}.
		 * @param decoder the decoder to use
		 */
		public void jackson2Decoder(Jackson2JsonDecoder decoder) {
			this.readers.put(Jackson2JsonDecoder.class, new DecoderHttpMessageReader<>(decoder));
		}

		/**
		 * Override the default Jackson {@code Encoder} for JSON.
		 * @param encoder the encoder to use
		 */
		public void jackson2Encoder(Jackson2JsonEncoder encoder) {
			this.writers.put(Jackson2JsonEncoder.class, new EncoderHttpMessageWriter<>(encoder));
		}


		// Accessors for internal use...


		protected Map<Class<?>, HttpMessageReader<?>> getReaders() {
			return this.readers;
		}

		protected Map<Class<?>, HttpMessageWriter<?>> getWriters() {
			return this.writers;
		}

		private void setSuppressed(boolean suppressed) {
			this.suppressed = suppressed;
		}


		// Protected methods for building a list of default readers or writers...


		protected <T, D extends Decoder<T>> void addReaderTo(List<HttpMessageReader<?>> result,
				Class<D> key, Supplier<D> fallback) {

			addReaderTo(result, () -> findDecoderReader(key, fallback));
		}

		protected void addReaderTo(List<HttpMessageReader<?>> result,
				Supplier<HttpMessageReader<?>> reader) {

			if (!this.suppressed) {
				result.add(reader.get());
			}
		}

		protected <T, D extends Decoder<T>> DecoderHttpMessageReader<?> findDecoderReader(
				Class<D> decoderType, Supplier<D> fallback) {

			DecoderHttpMessageReader<?> reader = (DecoderHttpMessageReader<?>) this.readers.get(decoderType);
			return reader != null ? reader : new DecoderHttpMessageReader<>(fallback.get());
		}

		@SuppressWarnings("unchecked")
		protected HttpMessageReader<?> findReader(Class<?> key, Supplier<HttpMessageReader<?>> fallback) {
			return this.readers.containsKey(key) ? this.readers.get(key) : fallback.get();
		}


		protected <T, E extends Encoder<T>> void addWriterTo(List<HttpMessageWriter<?>> result,
				Class<E> key, Supplier<E> fallback) {

			addWriterTo(result, () -> findEncoderWriter(key, fallback));
		}

		protected void addWriterTo(List<HttpMessageWriter<?>> result,
				Supplier<HttpMessageWriter<?>> writer) {

			if (!this.suppressed) {
				result.add(writer.get());
			}
		}

		protected <T, E extends Encoder<T>> EncoderHttpMessageWriter<?> findEncoderWriter(
				Class<E> encoderType, Supplier<E> fallback) {

			EncoderHttpMessageWriter<?> writer = (EncoderHttpMessageWriter<?>) this.writers.get(encoderType);
			return writer != null ? writer : new EncoderHttpMessageWriter<>(fallback.get());
		}

		@SuppressWarnings("unchecked")
		protected HttpMessageWriter<?> findWriter(Class<?> key, Supplier<HttpMessageWriter<?>> fallback) {
			return this.writers.containsKey(key) ? this.writers.get(key) : fallback.get();
		}


		protected void addStringReaderTextOnlyTo(List<HttpMessageReader<?>> result) {
			addReaderTo(result, () -> new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly(true)));
		}

		protected void addStringReaderTo(List<HttpMessageReader<?>> result) {
			addReaderTo(result, () -> new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes(true)));
		}

		protected void addStringWriterTextPlainOnlyTo(List<HttpMessageWriter<?>> result) {
			addWriterTo(result, () -> new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
		}

		protected void addStringWriterTo(List<HttpMessageWriter<?>> result) {
			addWriterTo(result, () -> new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
		}
	}

	/**
	 * Registry and container for custom HTTP message readers and writers.
	 */
	public static class CustomCodecConfigurer {

		private final List<HttpMessageReader<?>> typedReaders = new ArrayList<>();

		private final List<HttpMessageWriter<?>> typedWriters = new ArrayList<>();

		private final List<HttpMessageReader<?>> objectReaders = new ArrayList<>();

		private final List<HttpMessageWriter<?>> objectWriters = new ArrayList<>();


		/**
		 * Add a custom {@code Decoder} internally wrapped with
		 * {@link DecoderHttpMessageReader}).
		 */
		public void decoder(Decoder<?> decoder) {
			reader(new DecoderHttpMessageReader<>(decoder));
		}

		/**
		 * Add a custom {@code Encoder}, internally wrapped with
		 * {@link EncoderHttpMessageWriter}.
		 */
		public void encoder(Encoder<?> encoder) {
			writer(new EncoderHttpMessageWriter<>(encoder));
		}

		/**
		 * Add a custom {@link HttpMessageReader}. For readers of type
		 * {@link DecoderHttpMessageReader} consider using the shortcut
		 * {@link #decoder(Decoder)} instead.
		 */
		public void reader(HttpMessageReader<?> reader) {
			boolean canReadToObject = reader.canRead(ResolvableType.forClass(Object.class), null);
			(canReadToObject ? this.objectReaders : this.typedReaders).add(reader);
		}

		/**
		 * Add a custom {@link HttpMessageWriter}. For readers of type
		 * {@link EncoderHttpMessageWriter} consider using the shortcut
		 * {@link #encoder(Encoder)} instead.
		 */
		public void writer(HttpMessageWriter<?> writer) {
			boolean canWriteObject = writer.canWrite(ResolvableType.forClass(Object.class), null);
			(canWriteObject ? this.objectWriters : this.typedWriters).add(writer);
		}


		// Internal methods for building a list of custom readers or writers...

		protected void addTypedReadersTo(List<HttpMessageReader<?>> result) {
			result.addAll(this.typedReaders);
		}

		protected void addObjectReadersTo(List<HttpMessageReader<?>> result) {
			result.addAll(this.objectReaders);
		}

		protected void addTypedWritersTo(List<HttpMessageWriter<?>> result) {
			result.addAll(this.typedWriters);
		}

		protected void addObjectWritersTo(List<HttpMessageWriter<?>> result) {
			result.addAll(this.objectWriters);
		}
	}
}
