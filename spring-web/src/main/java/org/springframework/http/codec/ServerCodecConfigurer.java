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
import org.springframework.util.ClassUtils;

/**
 * Helps to configure a list of server-side HTTP message readers and writers
 * with support for built-in defaults and options to register additional custom
 * readers and writers via {@link #customCodec()}.
 *
 * <p>The built-in defaults include basic data types such as
 * {@link Byte byte[]}, {@link java.nio.ByteBuffer ByteBuffer},
 * {@link org.springframework.core.io.buffer.DataBuffer DataBuffer},
 * {@link String}, {@link org.springframework.core.io.Resource Resource},
 * in addition to JAXB2 and Jackson 2 based on classpath detection, as well as
 * support for Server-Sent Events. There are options to {@link #defaultCodec()
 * override} some of the defaults or to have them
 * {@link #registerDefaults(boolean) turned off} completely.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServerCodecConfigurer {

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					ServerCodecConfigurer.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
							ServerCodecConfigurer.class.getClassLoader());

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder", ServerCodecConfigurer.class.getClassLoader());


	private final DefaultCodecConfigurer defaultCodecs = new DefaultCodecConfigurer();

	private final CustomCodecConfigurer customCodecs = new CustomCodecConfigurer();


	/**
	 * Provide overrides for built-in HTTP message readers and writers.
	 */
	public DefaultCodecConfigurer defaultCodec() {
		return this.defaultCodecs;
	}

	/**
	 * Whether to make default HTTP message reader and writer registrations.
	 * <p>By default this is set to {@code "true"}.
	 */
	public void registerDefaults(boolean register) {
		this.defaultCodec().setSuppressed(!register);
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
	public List<ServerHttpMessageReader<?>> getReaders() {

		// Built-in, concrete Java type readers
		List<ServerHttpMessageReader<?>> result = new ArrayList<>();
		this.defaultCodecs.addReaderTo(result, ByteArrayDecoder.class, ByteArrayDecoder::new);
		this.defaultCodecs.addReaderTo(result, ByteBufferDecoder.class, ByteBufferDecoder::new);
		this.defaultCodecs.addReaderTo(result, DataBufferDecoder.class, DataBufferDecoder::new);
		this.defaultCodecs.addReaderTo(result, ResourceDecoder.class, ResourceDecoder::new);
		this.defaultCodecs.addStringReaderTextOnlyTo(result);

		// Custom, concrete Java type readers
		this.customCodecs.addTypedReadersTo(result);

		// Built-in, Object-based readers
		if (jaxb2Present) {
			this.defaultCodecs.addReaderTo(result, Jaxb2XmlDecoder.class, Jaxb2XmlDecoder::new);
		}
		if (jackson2Present) {
			this.defaultCodecs.addReaderTo(result, Jackson2JsonDecoder.class, Jackson2JsonDecoder::new);
		}

		// Custom, Object-based readers
		this.customCodecs.addObjectReadersTo(result);

		// Potentially overlapping Java types + "*/*"
		this.defaultCodecs.addStringReaderTo(result);
		return result;
	}

	/**
	 * Prepare a list of HTTP message writers.
	 */
	public List<ServerHttpMessageWriter<?>> getWriters() {

		// Built-in, concrete Java type readers
		List<ServerHttpMessageWriter<?>> result = new ArrayList<>();
		this.defaultCodecs.addWriterTo(result, ByteArrayEncoder.class, ByteArrayEncoder::new);
		this.defaultCodecs.addWriterTo(result, ByteBufferEncoder.class, ByteBufferEncoder::new);
		this.defaultCodecs.addWriterTo(result, DataBufferEncoder.class, DataBufferEncoder::new);
		this.defaultCodecs.addWriterTo(result, ResourceHttpMessageWriter::new);
		this.defaultCodecs.addStringWriterTextPlainOnlyTo(result);

		// Custom, concrete Java type readers
		this.customCodecs.addTypedWritersTo(result);

		// Built-in, Object-based readers
		if (jaxb2Present) {
			this.defaultCodecs.addWriterTo(result, Jaxb2XmlEncoder.class, Jaxb2XmlEncoder::new);
		}
		if (jackson2Present) {
			this.defaultCodecs.addWriterTo(result, Jackson2JsonEncoder.class, Jackson2JsonEncoder::new);
		}
		this.defaultCodecs.addSseWriterTo(result);

		// Custom, Object-based readers
		this.customCodecs.addObjectWritersTo(result);

		// Potentially overlapping Java types + "*/*"
		this.defaultCodecs.addStringWriterTo(result);
		return result;
	}


	/**
	 * A registry and a factory for built-in HTTP message readers and writers.
	 */
	public static class DefaultCodecConfigurer {

		private boolean suppressed = false;

		private final Map<Class<?>, ServerHttpMessageReader<?>> readers = new HashMap<>();

		private final Map<Class<?>, ServerHttpMessageWriter<?>> writers = new HashMap<>();


		/**
		 * Override the default Jackson {@code Decoder}.
		 * @param decoder the decoder to use
		 */
		public void jackson2Decoder(Jackson2JsonDecoder decoder) {
			this.readers.put(Jackson2JsonDecoder.class, new DecoderHttpMessageReader<>(decoder));
		}

		/**
		 * Override the default Jackson {@code Encoder} for JSON. Also used for
		 * SSE unless further overridden via {@link #sse(Encoder)}.
		 * @param encoder the encoder to use
		 */
		public void jackson2Encoder(Jackson2JsonEncoder encoder) {
			this.writers.put(Jackson2JsonEncoder.class, new EncoderHttpMessageWriter<>(encoder));
		}

		/**
		 * Configure the {@code Encoder} to use for Server-Sent Events.
		 * <p>By default the {@link #jackson2Encoder} override is used for SSE.
		 * @param encoder the encoder to use
		 */
		public void sse(Encoder<?> encoder) {
			ServerHttpMessageWriter<?> writer = new ServerSentEventHttpMessageWriter(encoder);
			this.writers.put(ServerSentEventHttpMessageWriter.class, writer);
		}


		// Internal methods for building a list of default readers or writers...

		private void setSuppressed(boolean suppressed) {
			this.suppressed = suppressed;
		}

		private <T, D extends Decoder<T>> void addReaderTo(List<ServerHttpMessageReader<?>> result,
				Class<D> key, Supplier<D> fallback) {

			addReaderTo(result, () -> findReader(key, fallback));
		}

		private void addReaderTo(List<ServerHttpMessageReader<?>> result,
				Supplier<ServerHttpMessageReader<?>> reader) {

			if (!this.suppressed) {
				result.add(reader.get());
			}
		}

		private <T, D extends Decoder<T>> DecoderHttpMessageReader<?> findReader(
				Class<D> key, Supplier<D> fallback) {

			DecoderHttpMessageReader<?> reader = (DecoderHttpMessageReader<?>) this.readers.get(key);
			return reader != null ? reader : new DecoderHttpMessageReader<>(fallback.get());
		}


		private <T, E extends Encoder<T>> void addWriterTo(List<ServerHttpMessageWriter<?>> result,
				Class<E> key, Supplier<E> fallback) {

			addWriterTo(result, () -> findWriter(key, fallback));
		}

		private void addWriterTo(List<ServerHttpMessageWriter<?>> result,
				Supplier<ServerHttpMessageWriter<?>> writer) {

			if (!this.suppressed) {
				result.add(writer.get());
			}
		}

		private <T, E extends Encoder<T>> EncoderHttpMessageWriter<?> findWriter(
				Class<E> key, Supplier<E> fallback) {

			EncoderHttpMessageWriter<?> writer = (EncoderHttpMessageWriter<?>) this.writers.get(key);
			return writer != null ? writer : new EncoderHttpMessageWriter<>(fallback.get());
		}


		private void addStringReaderTextOnlyTo(List<ServerHttpMessageReader<?>> result) {
			addReaderTo(result, () -> new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly(true)));
		}

		private void addStringReaderTo(List<ServerHttpMessageReader<?>> result) {
			addReaderTo(result, () -> new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes(true)));
		}

		private void addStringWriterTextPlainOnlyTo(List<ServerHttpMessageWriter<?>> result) {
			addWriterTo(result, () -> new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
		}

		private void addStringWriterTo(List<ServerHttpMessageWriter<?>> result) {
			addWriterTo(result, () -> new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
		}

		private void addSseWriterTo(List<ServerHttpMessageWriter<?>> result) {
			addWriterTo(result, () -> {
				ServerHttpMessageWriter<?> writer = this.writers.get(ServerSentEventHttpMessageWriter.class);
				if (writer != null) {
					return writer;
				}
				if (jackson2Present) {
					return new ServerSentEventHttpMessageWriter(
							findWriter(Jackson2JsonEncoder.class, Jackson2JsonEncoder::new).getEncoder());
				}
				return new ServerSentEventHttpMessageWriter();
			});
		}
	}

	/**
	 * Registry and container for custom HTTP message readers and writers.
	 */
	public static class CustomCodecConfigurer {

		private final List<ServerHttpMessageReader<?>> typedReaders = new ArrayList<>();

		private final List<ServerHttpMessageWriter<?>> typedWriters = new ArrayList<>();

		private final List<ServerHttpMessageReader<?>> objectReaders = new ArrayList<>();

		private final List<ServerHttpMessageWriter<?>> objectWriters = new ArrayList<>();


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
		 * Add a custom {@link ServerHttpMessageReader}. For readers of type
		 * {@link DecoderHttpMessageReader} consider using the shortcut
		 * {@link #decoder(Decoder)} instead.
		 */
		public void reader(ServerHttpMessageReader<?> reader) {
			boolean canReadToObject = reader.canRead(ResolvableType.forClass(Object.class), null);
			(canReadToObject ? this.objectReaders : this.typedReaders).add(reader);
		}

		/**
		 * Add a custom {@link ServerHttpMessageWriter}. For readers of type
		 * {@link EncoderHttpMessageWriter} consider using the shortcut
		 * {@link #encoder(Encoder)} instead.
		 */
		public void writer(ServerHttpMessageWriter<?> writer) {
			boolean canWriteObject = writer.canWrite(ResolvableType.forClass(Object.class), null);
			(canWriteObject ? this.objectWriters : this.typedWriters).add(writer);
		}


		// Internal methods for building a list of custom readers or writers...

		private void addTypedReadersTo(List<ServerHttpMessageReader<?>> result) {
			result.addAll(this.typedReaders);
		}

		private void addObjectReadersTo(List<ServerHttpMessageReader<?>> result) {
			result.addAll(this.objectReaders);
		}

		private void addTypedWritersTo(List<ServerHttpMessageWriter<?>> result) {
			result.addAll(this.typedWriters);
		}

		private void addObjectWritersTo(List<ServerHttpMessageWriter<?>> result) {
			result.addAll(this.objectWriters);
		}
	}

}
