/*
 * Copyright 2002-present the original author or authors.
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
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.util.TokenBuffer;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Abstract base class for Jackson 3.x decoding, leveraging non-blocking parsing.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 * @param <T> the type of {@link ObjectMapper}
 */
public abstract class AbstractJacksonDecoder<T extends ObjectMapper> extends JacksonCodecSupport<T> implements HttpMessageDecoder<Object> {

	private int maxInMemorySize = 256 * 1024;


	/**
	 * Construct a new instance with the provided {@link MapperBuilder builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s found
	 * by {@link MapperBuilder#findModules(ClassLoader)} and {@link MimeType}s.
	 */
	protected AbstractJacksonDecoder(MapperBuilder<T, ?> builder, MimeType... mimeTypes) {
		super(builder, mimeTypes);
	}

	/**
	 * Construct a new instance with the provided {@link ObjectMapper} and {@link MimeType}s.
	 */
	protected AbstractJacksonDecoder(T mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}

	/**
	 * Set the max number of bytes that can be buffered by this decoder. This
	 * is either the size of the entire input when decoding as a whole, or the
	 * size of one top-level JSON object within a JSON stream. When the limit
	 * is exceeded, {@link DataBufferLimitException} is raised.
	 * <p>By default this is set to 256K.
	 * @param byteCount the max number of bytes to buffer, or -1 for unlimited
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	/**
	 * Return the {@link #setMaxInMemorySize configured} byte count limit.
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		if (!supportsMimeType(mimeType)) {
			return false;
		}
		T mapper = selectMapper(elementType, mimeType);
		if (mapper == null) {
			return false;
		}
		return !CharSequence.class.isAssignableFrom(elementType.toClass());
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		T mapper = selectMapper(elementType, mimeType);
		if (mapper == null) {
			return Flux.error(new IllegalStateException("No ObjectMapper for " + elementType));
		}

		boolean forceUseOfBigDecimal = mapper.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
		if (BigDecimal.class.equals(elementType.getType())) {
			forceUseOfBigDecimal = true;
		}

		boolean tokenizeArrays = (!elementType.isArray() &&
				!Collection.class.isAssignableFrom(elementType.resolve(Object.class)));

		Flux<DataBuffer> processed = processInput(input, elementType, mimeType, hints);
		Flux<TokenBuffer> tokens = JacksonTokenizer.tokenize(processed, mapper,
				tokenizeArrays, forceUseOfBigDecimal, getMaxInMemorySize());

		return Flux.deferContextual(contextView -> {

			Map<String, Object> hintsToUse = contextView.isEmpty() ? hints :
					Hints.merge(hints, ContextView.class.getName(), contextView);

			ObjectReader reader = createObjectReader(mapper, elementType, hintsToUse);

			return tokens.handle((tokenBuffer, sink) -> {
				try {
					Object value = reader.readValue(tokenBuffer.asParser(getMapper()._deserializationContext()));
					logValue(value, hints);
					if (value != null) {
						sink.next(value);
					}
				}
				catch (JacksonException ex) {
					sink.error(processException(ex));
				}
			})
			.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
		});
	}

	/**
	 * Process the input publisher into a flux. Default implementation returns
	 * {@link Flux#from(Publisher)}, but subclasses can choose to customize
	 * this behavior.
	 * @param input the {@code DataBuffer} input stream to process
	 * @param elementType the expected type of elements in the output stream
	 * @param mimeType the MIME type associated with the input stream (optional)
	 * @param hints additional information about how to do encode
	 * @return the processed flux
	 */
	protected Flux<DataBuffer> processInput(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(input);
	}

	@Override
	public Mono<Object> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Mono.deferContextual(contextView -> {

			Map<String, Object> hintsToUse = contextView.isEmpty() ? hints :
					Hints.merge(hints, ContextView.class.getName(), contextView);

			return DataBufferUtils.join(input, this.maxInMemorySize).flatMap(dataBuffer ->
					Mono.justOrEmpty(decode(dataBuffer, elementType, mimeType, hintsToUse)));
		});
	}

	@Override
	public Object decode(DataBuffer dataBuffer, ResolvableType targetType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {

		T mapper = selectMapper(targetType, mimeType);
		if (mapper == null) {
			throw new IllegalStateException("No ObjectMapper for " + targetType);
		}

		try {
			ObjectReader objectReader = createObjectReader(mapper, targetType, hints);
			Object value = objectReader.readValue(dataBuffer.asInputStream());
			logValue(value, hints);
			return value;
		}
		catch (JacksonException ex) {
			throw processException(ex);
		}
		finally {
			DataBufferUtils.release(dataBuffer);
		}
	}

	private ObjectReader createObjectReader(T mapper, ResolvableType elementType, @Nullable Map<String, Object> hints) {

		Assert.notNull(elementType, "'elementType' must not be null");
		Class<?> contextClass = getContextClass(elementType);
		if (contextClass == null && hints != null) {
			contextClass = getContextClass((ResolvableType) hints.get(ACTUAL_TYPE_HINT));
		}
		JavaType javaType = getJavaType(elementType.getType(), contextClass);
		Class<?> jsonView = (hints != null ? (Class<?>) hints.get(JacksonCodecSupport.JSON_VIEW_HINT) : null);

		ObjectReader objectReader = (jsonView != null ?
				mapper.readerWithView(jsonView).forType(javaType) :
				mapper.readerFor(javaType));

		return customizeReader(objectReader, elementType, hints);
	}

	/**
	 * Subclasses can use this method to customize {@link ObjectReader} used
	 * for reading values.
	 * @param reader the reader instance to customize
	 * @param elementType the target type of element values to read to
	 * @param hints a map with serialization hints;
	 * the Reactor Context, when available, may be accessed under the key
	 * {@code ContextView.class.getName()}
	 * @return the customized {@code ObjectReader} to use
	 */
	protected ObjectReader customizeReader(
			ObjectReader reader, ResolvableType elementType, @Nullable Map<String, Object> hints) {

		return reader;
	}

	private @Nullable Class<?> getContextClass(@Nullable ResolvableType elementType) {
		MethodParameter param = (elementType != null ? getParameter(elementType) : null);
		return (param != null ? param.getContainingClass() : null);
	}

	private void logValue(@Nullable Object value, @Nullable Map<String, Object> hints) {
		if (!Hints.isLoggingSuppressed(hints)) {
			LogFormatUtils.traceDebug(logger, traceOn -> {
				String formatted = LogFormatUtils.formatValue(value, !traceOn);
				return Hints.getLogPrefix(hints) + "Decoded [" + formatted + "]";
			});
		}
	}

	private CodecException processException(JacksonException ex) {
		if (ex instanceof InvalidDefinitionException ide) {
			JavaType type = ide.getType();
			return new CodecException("Type definition error: " + type, ex);
		}
		if (ex instanceof JacksonIOException) {
			return new DecodingException("I/O error while parsing input stream", ex);
		}
		String originalMessage = ex.getOriginalMessage();
		return new DecodingException("JSON decoding error: " + originalMessage, ex);
	}


	// HttpMessageDecoder

	@Override
	public Map<String, Object> getDecodeHints(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response) {

		return getHints(actualType);
	}

	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return getMimeTypes();
	}

	@Override
	public List<MimeType> getDecodableMimeTypes(ResolvableType targetType) {
		return getMimeTypes(targetType);
	}

	// JacksonCodecSupport

	@Override
	protected <A extends Annotation> @Nullable A getAnnotation(MethodParameter parameter, Class<A> annotType) {
		return parameter.getParameterAnnotation(annotType);
	}

}
