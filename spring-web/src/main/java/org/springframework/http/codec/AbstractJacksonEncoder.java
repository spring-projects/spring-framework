/*
 * Copyright 2002-2025 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.util.ByteArrayBuilder;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.ser.FilterProvider;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;

/**
 * Base class providing support methods for Jackson 3.x encoding. For non-streaming use
 * cases, {@link Flux} elements are collected into a {@link List} before serialization for
 * performance reasons.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
public abstract class AbstractJacksonEncoder extends JacksonCodecSupport implements HttpMessageEncoder<Object> {

	private static final byte[] NEWLINE_SEPARATOR = {'\n'};

	private static final byte[] EMPTY_BYTES = new byte[0];

	private static final Map<String, JsonEncoding> ENCODINGS;

	static {
		ENCODINGS = CollectionUtils.newHashMap(JsonEncoding.values().length);
		for (JsonEncoding encoding : JsonEncoding.values()) {
			ENCODINGS.put(encoding.getJavaName(), encoding);
		}
		ENCODINGS.put("US-ASCII", JsonEncoding.UTF8);
	}


	private final List<MediaType> streamingMediaTypes = new ArrayList<>(1);


	/**
	 * Construct a new instance with the provided {@link MapperBuilder builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s found
	 * by {@link MapperBuilder#findModules(ClassLoader)} and {@link MimeType}s.
	 */
	protected AbstractJacksonEncoder(MapperBuilder<?, ?> builder, MimeType... mimeTypes) {
		super(builder, mimeTypes);
	}

	/**
	 * Construct a new instance with the provided {@link ObjectMapper} and {@link MimeType}s.
	 */
	protected AbstractJacksonEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}

	/**
	 * Configure "streaming" media types for which flushing should be performed
	 * automatically vs at the end of the stream.
	 */
	public void setStreamingMediaTypes(List<MediaType> mediaTypes) {
		this.streamingMediaTypes.clear();
		this.streamingMediaTypes.addAll(mediaTypes);
	}

	@Override
	@SuppressWarnings("removal")
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		if (!supportsMimeType(mimeType)) {
			return false;
		}
		if (mimeType != null && mimeType.getCharset() != null) {
			Charset charset = mimeType.getCharset();
			if (!ENCODINGS.containsKey(charset.name())) {
				return false;
			}
		}
		if (this.objectMapperRegistrations != null && selectObjectMapper(elementType, mimeType) == null) {
			return false;
		}
		Class<?> clazz = elementType.resolve();
		if (clazz == null) {
			return true;
		}
		if (MappingJacksonValue.class.isAssignableFrom(elementType.resolve(clazz))) {
			throw new UnsupportedOperationException("MappingJacksonValue is not supported, use hints instead");
		}
		return !String.class.isAssignableFrom(elementType.resolve(clazz));
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");

		return Flux.deferContextual(contextView -> {

			Map<String, Object> hintsToUse = contextView.isEmpty() ? hints :
					Hints.merge(hints, ContextView.class.getName(), contextView);

			if (inputStream instanceof Mono) {
				return Mono.from(inputStream)
						.map(value -> encodeValue(value, bufferFactory, elementType, mimeType, hintsToUse))
						.flux();
			}

			try {
				ObjectMapper mapper = selectObjectMapper(elementType, mimeType);
				if (mapper == null) {
					throw new IllegalStateException("No ObjectMapper for " + elementType);
				}

				ObjectWriter writer = createObjectWriter(mapper, elementType, mimeType, null, hintsToUse);
				ByteArrayBuilder byteBuilder = new ByteArrayBuilder(writer.generatorFactory()._getBufferRecycler());
				JsonEncoding encoding = getJsonEncoding(mimeType);
				JsonGenerator generator = mapper.createGenerator(byteBuilder, encoding);
				SequenceWriter sequenceWriter = writer.writeValues(generator);

				byte[] separator = getStreamingMediaTypeSeparator(mimeType);
				Flux<DataBuffer> dataBufferFlux;

				if (separator != null) {
					dataBufferFlux = Flux.from(inputStream).map(value -> encodeStreamingValue(
							value, bufferFactory, hintsToUse, sequenceWriter, byteBuilder, EMPTY_BYTES, separator));
				}
				else {
					JsonArrayJoinHelper helper = new JsonArrayJoinHelper();

					// Do not prepend JSON array prefix until first signal is known, onNext vs onError
					// Keeps response not committed for error handling

					dataBufferFlux = Flux.from(inputStream)
							.map(value -> {
								byte[] prefix = helper.getPrefix();
								byte[] delimiter = helper.getDelimiter();

								DataBuffer dataBuffer = encodeStreamingValue(
										value, bufferFactory, hintsToUse, sequenceWriter, byteBuilder,
										delimiter, EMPTY_BYTES);

								return (prefix.length > 0 ?
										bufferFactory.join(List.of(bufferFactory.wrap(prefix), dataBuffer)) :
										dataBuffer);
							})
							.switchIfEmpty(Mono.fromCallable(() -> bufferFactory.wrap(helper.getPrefix())))
							.concatWith(Mono.fromCallable(() -> bufferFactory.wrap(helper.getSuffix())));
				}

				return dataBufferFlux
						.doOnNext(dataBuffer -> Hints.touchDataBuffer(dataBuffer, hintsToUse, logger))
						.doAfterTerminate(() -> {
							try {
								generator.close();
								byteBuilder.release();
							}
							catch (JacksonIOException ex) {
								logger.error("Could not close Encoder resources", ex);
							}
						});
			}
			catch (JacksonIOException ex) {
				return Flux.error(ex);
			}
		});
	}

	@Override
	public DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory,
			ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Class<?> jsonView = null;
		FilterProvider filters = null;
		if (hints != null) {
			jsonView = (Class<?>) hints.get(JSON_VIEW_HINT);
			filters = (FilterProvider) hints.get(FILTER_PROVIDER_HINT);
		}

		ObjectMapper mapper = selectObjectMapper(valueType, mimeType);
		if (mapper == null) {
			throw new IllegalStateException("No ObjectMapper for " + valueType);
		}

		ObjectWriter writer = createObjectWriter(mapper, valueType, mimeType, jsonView, hints);
		if (filters != null) {
			writer = writer.with(filters);
		}

		ByteArrayBuilder byteBuilder = new ByteArrayBuilder(writer.generatorFactory()._getBufferRecycler());
		try {
			JsonEncoding encoding = getJsonEncoding(mimeType);

			logValue(hints, value);

			try (JsonGenerator generator = writer.createGenerator(byteBuilder, encoding)) {
				writer.writeValue(generator, value);
				generator.flush();
			}
			catch (InvalidDefinitionException ex) {
				throw new CodecException("Type definition error: " + ex.getType(), ex);
			}
			catch (JacksonException ex) {
				throw new EncodingException("JSON encoding error: " + ex.getOriginalMessage(), ex);
			}

			byte[] bytes = byteBuilder.toByteArray();
			DataBuffer buffer = bufferFactory.allocateBuffer(bytes.length);
			buffer.write(bytes);
			Hints.touchDataBuffer(buffer, hints, logger);

			return buffer;
		}
		finally {
			byteBuilder.release();
		}
	}

	private DataBuffer encodeStreamingValue(
			Object value, DataBufferFactory bufferFactory, @Nullable Map<String, Object> hints,
			SequenceWriter sequenceWriter, ByteArrayBuilder byteArrayBuilder,
			byte[] prefix, byte[] suffix) {

		logValue(hints, value);

		try {
			sequenceWriter.write(value);
			sequenceWriter.flush();
		}
		catch (InvalidDefinitionException ex) {
			throw new CodecException("Type definition error: " + ex.getType(), ex);
		}
		catch (JacksonException ex) {
			throw new EncodingException("JSON encoding error: " + ex.getOriginalMessage(), ex);
		}

		byte[] bytes = byteArrayBuilder.toByteArray();
		byteArrayBuilder.reset();

		int offset;
		int length;
		if (bytes.length > 0 && bytes[0] == ' ') {
			// SequenceWriter writes an unnecessary space in between values
			offset = 1;
			length = bytes.length - 1;
		}
		else {
			offset = 0;
			length = bytes.length;
		}
		DataBuffer buffer = bufferFactory.allocateBuffer(length + prefix.length + suffix.length);
		if (prefix.length != 0) {
			buffer.write(prefix);
		}
		buffer.write(bytes, offset, length);
		if (suffix.length != 0) {
			buffer.write(suffix);
		}
		Hints.touchDataBuffer(buffer, hints, logger);

		return buffer;
	}

	private void logValue(@Nullable Map<String, Object> hints, Object value) {
		if (!Hints.isLoggingSuppressed(hints)) {
			LogFormatUtils.traceDebug(logger, traceOn -> {
				String formatted = LogFormatUtils.formatValue(value, !traceOn);
				return Hints.getLogPrefix(hints) + "Encoding [" + formatted + "]";
			});
		}
	}

	private ObjectWriter createObjectWriter(
			ObjectMapper mapper, ResolvableType valueType, @Nullable MimeType mimeType,
			@Nullable Class<?> jsonView, @Nullable Map<String, Object> hints) {

		JavaType javaType = getJavaType(valueType.getType(), null);
		if (jsonView == null && hints != null) {
			jsonView = (Class<?>) hints.get(JacksonCodecSupport.JSON_VIEW_HINT);
		}
		ObjectWriter writer = (jsonView != null ? mapper.writerWithView(jsonView) : mapper.writer());
		if (javaType.isContainerType()) {
			writer = writer.forType(javaType);
		}
		return customizeWriter(writer, mimeType, valueType, hints);
	}

	/**
	 * Subclasses can use this method to customize the {@link ObjectWriter} used
	 * for writing values.
	 * @param writer the writer instance to customize
	 * @param mimeType the selected MIME type
	 * @param elementType the type of element values to write
	 * @param hints a map with serialization hints; the Reactor Context, when
	 * available, may be accessed under the key
	 * {@code ContextView.class.getName()}
	 * @return the customized {@code ObjectWriter} to use
	 */
	protected ObjectWriter customizeWriter(ObjectWriter writer, @Nullable MimeType mimeType,
			ResolvableType elementType, @Nullable Map<String, Object> hints) {

		return writer;
	}

	/**
	 * Return the separator to use for the given mime type.
	 * <p>By default, this method returns new line {@code "\n"} if the given
	 * mime type is one of the configured {@link #setStreamingMediaTypes(List)
	 * streaming} mime types.
	 */
	protected byte @Nullable [] getStreamingMediaTypeSeparator(@Nullable MimeType mimeType) {
		for (MediaType streamingMediaType : this.streamingMediaTypes) {
			if (streamingMediaType.isCompatibleWith(mimeType)) {
				return NEWLINE_SEPARATOR;
			}
		}
		return null;
	}

	/**
	 * Determine the JSON encoding to use for the given mime type.
	 * @param mimeType the mime type as requested by the caller
	 * @return the JSON encoding to use (never {@code null})
	 */
	protected JsonEncoding getJsonEncoding(@Nullable MimeType mimeType) {
		if (mimeType != null && mimeType.getCharset() != null) {
			Charset charset = mimeType.getCharset();
			JsonEncoding result = ENCODINGS.get(charset.name());
			if (result != null) {
				return result;
			}
		}
		return JsonEncoding.UTF8;
	}


	// HttpMessageEncoder

	@Override
	public List<MimeType> getEncodableMimeTypes() {
		return getMimeTypes();
	}

	@Override
	public List<MimeType> getEncodableMimeTypes(ResolvableType elementType) {
		return getMimeTypes(elementType);
	}

	@Override
	public List<MediaType> getStreamingMediaTypes() {
		return Collections.unmodifiableList(this.streamingMediaTypes);
	}

	@Override
	public Map<String, Object> getEncodeHints(@Nullable ResolvableType actualType, ResolvableType elementType,
			@Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		return (actualType != null ? getHints(actualType) : Hints.none());
	}


	// JacksonCodecSupport

	@Override
	protected <A extends Annotation> @Nullable A getAnnotation(MethodParameter parameter, Class<A> annotType) {
		return parameter.getMethodAnnotation(annotType);
	}


	private static class JsonArrayJoinHelper {

		private static final byte[] COMMA_SEPARATOR = {','};

		private static final byte[] OPEN_BRACKET = {'['};

		private static final byte[] CLOSE_BRACKET = {']'};

		private boolean firstItemEmitted;

		public byte[] getDelimiter() {
			if (this.firstItemEmitted) {
				return COMMA_SEPARATOR;
			}
			this.firstItemEmitted = true;
			return EMPTY_BYTES;
		}

		public byte[] getPrefix() {
			return (this.firstItemEmitted ? EMPTY_BYTES : OPEN_BRACKET);
		}

		public byte[] getSuffix() {
			return CLOSE_BRACKET;
		}
	}

}
