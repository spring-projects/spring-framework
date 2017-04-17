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
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of {@link CodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultCodecConfigurer implements CodecConfigurer {

	static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					org.springframework.http.codec.DefaultCodecConfigurer.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
							org.springframework.http.codec.DefaultCodecConfigurer.class
									.getClassLoader());

	static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder",
					org.springframework.http.codec.DefaultCodecConfigurer.class.getClassLoader());


	private final AbstractDefaultCodecsConfigurer defaultCodecs;

	private final CustomCodecConfigurer customCodecs = new CustomCodecConfigurer();


	/**
	 * Protected constructor with the configurer for default readers and writers.
	 */
	DefaultCodecConfigurer(AbstractDefaultCodecsConfigurer defaultCodecConfigurer) {
		Assert.notNull(defaultCodecConfigurer, "DefaultCodecConfigurer is required.");
		this.defaultCodecs = defaultCodecConfigurer;
	}


	@Override
	public DefaultCodecsConfigurer defaultCodecs() {
		return this.defaultCodecs;
	}

	@Override
	public void registerDefaults(boolean registerDefaults) {
		this.defaultCodecs.setSuppressed(!registerDefaults);
	}

	@Override
	public CustomCodecsConfigurer customCodecs() {
		return this.customCodecs;
	}


	@Override
	public List<HttpMessageReader<?>> getReaders() {
		List<HttpMessageReader<?>> result = new ArrayList<>();

		this.defaultCodecs.addTypedReadersTo(result);
		this.customCodecs.addTypedReadersTo(result);

		this.defaultCodecs.addObjectReadersTo(result);
		this.customCodecs.addObjectReadersTo(result);

		// String + "*/*"
		this.defaultCodecs.addStringReaderTo(result);
		return result;
	}

	/**
	 * Prepare a list of HTTP message writers.
	 */
	@Override
	public List<HttpMessageWriter<?>> getWriters() {

		List<HttpMessageWriter<?>> result = new ArrayList<>();

		this.defaultCodecs.addTypedWritersTo(result);
		this.customCodecs.addTypedWritersTo(result);

		this.defaultCodecs.addObjectWritersTo(result);
		this.customCodecs.addObjectWritersTo(result);

		// String + "*/*"
		this.defaultCodecs.addStringWriterTo(result);
		return result;
	}


	/**
	 * Default implementation for {@link CodecConfigurer.DefaultCodecsConfigurer}.
	 */
	abstract static class AbstractDefaultCodecsConfigurer
			implements CodecConfigurer.DefaultCodecsConfigurer {

		private boolean suppressed = false;

		private final Map<Class<?>, HttpMessageReader<?>> readers = new HashMap<>();

		private final Map<Class<?>, HttpMessageWriter<?>> writers = new HashMap<>();

		@Override
		public void jackson2Decoder(Jackson2JsonDecoder decoder) {
			this.readers.put(Jackson2JsonDecoder.class, new DecoderHttpMessageReader<>(decoder));
		}

		@Override
		public void jackson2Encoder(Jackson2JsonEncoder encoder) {
			this.writers.put(Jackson2JsonEncoder.class, new EncoderHttpMessageWriter<>(encoder));
		}

		public void addTypedReadersTo(List<HttpMessageReader<?>> result) {
			addReaderTo(result, ByteArrayDecoder.class, ByteArrayDecoder::new);
			addReaderTo(result, ByteBufferDecoder.class, ByteBufferDecoder::new);
			addReaderTo(result, DataBufferDecoder.class, DataBufferDecoder::new);
			addReaderTo(result, ResourceDecoder.class, ResourceDecoder::new);
			addStringReaderTextOnlyTo(result);
		}

		protected void addTypedWritersTo(List<HttpMessageWriter<?>> result) {
			addWriterTo(result, ByteArrayEncoder.class, ByteArrayEncoder::new);
			addWriterTo(result, ByteBufferEncoder.class, ByteBufferEncoder::new);
			addWriterTo(result, DataBufferEncoder.class, DataBufferEncoder::new);
			addWriterTo(result, ResourceHttpMessageWriter::new);
			addStringWriterTextPlainOnlyTo(result);
		}


		protected void addObjectReadersTo(List<HttpMessageReader<?>> result) {
			if (jaxb2Present) {
				addReaderTo(result, Jaxb2XmlDecoder.class, Jaxb2XmlDecoder::new);
			}
			if (jackson2Present) {
				addReaderTo(result, Jackson2JsonDecoder.class, Jackson2JsonDecoder::new);
			}
		}

		protected void addObjectWritersTo(List<HttpMessageWriter<?>> result) {
			if (jaxb2Present) {
				addWriterTo(result, Jaxb2XmlEncoder.class, Jaxb2XmlEncoder::new);
			}
			if (jackson2Present) {
				addWriterTo(result, Jackson2JsonEncoder.class, Jackson2JsonEncoder::new);
			}
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


		protected abstract void addStringReaderTextOnlyTo(List<HttpMessageReader<?>> result);

		protected abstract void addStringReaderTo(List<HttpMessageReader<?>> result);

		protected void addStringWriterTextPlainOnlyTo(List<HttpMessageWriter<?>> result) {
			addWriterTo(result, () -> new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
		}

		protected void addStringWriterTo(List<HttpMessageWriter<?>> result) {
			addWriterTo(result, () -> new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
		}
	}


	/**
	 * Default implementation of CustomCodecsConfigurer.
	 */
	private static class CustomCodecConfigurer implements CodecConfigurer.CustomCodecsConfigurer {

		private final List<HttpMessageReader<?>> typedReaders = new ArrayList<>();

		private final List<HttpMessageWriter<?>> typedWriters = new ArrayList<>();

		private final List<HttpMessageReader<?>> objectReaders = new ArrayList<>();

		private final List<HttpMessageWriter<?>> objectWriters = new ArrayList<>();


		@Override
		public void decoder(Decoder<?> decoder) {
			reader(new DecoderHttpMessageReader<>(decoder));
		}

		@Override
		public void encoder(Encoder<?> encoder) {
			writer(new EncoderHttpMessageWriter<>(encoder));
		}

		@Override
		public void reader(HttpMessageReader<?> reader) {
			boolean canReadToObject = reader.canRead(ResolvableType.forClass(Object.class), null);
			(canReadToObject ? this.objectReaders : this.typedReaders).add(reader);
		}

		@Override
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
