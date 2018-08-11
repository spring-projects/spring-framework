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

package org.springframework.http.codec.json;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageEncoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Base class providing support methods for Jackson 2.9 encoding. For non-streaming use
 * cases, {@link Flux} elements are collected into a {@link List} before serialization for
 * performance reason.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class AbstractJackson2Encoder extends Jackson2CodecSupport implements HttpMessageEncoder<Object> {

	private static final byte[] NEWLINE_SEPARATOR = {'\n'};

	private static final Map<MediaType, byte[]> STREAM_SEPARATORS;

	static {
		STREAM_SEPARATORS = new HashMap<>();
		STREAM_SEPARATORS.put(MediaType.APPLICATION_STREAM_JSON, NEWLINE_SEPARATOR);
		STREAM_SEPARATORS.put(MediaType.parseMediaType("application/stream+x-jackson-smile"), new byte[0]);
	}


	private final List<MediaType> streamingMediaTypes = new ArrayList<>(1);


	/**
	 * Constructor with a Jackson {@link ObjectMapper} to use.
	 */
	protected AbstractJackson2Encoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}


	/**
	 * Configure "streaming" media types for which flushing should be performed
	 * automatically vs at the end of the stream.
	 * <p>By default this is set to {@link MediaType#APPLICATION_STREAM_JSON}.
	 * @param mediaTypes one or more media types to add to the list
	 * @see HttpMessageEncoder#getStreamingMediaTypes()
	 */
	public void setStreamingMediaTypes(List<MediaType> mediaTypes) {
		this.streamingMediaTypes.clear();
		this.streamingMediaTypes.addAll(mediaTypes);
	}


	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		Class<?> clazz = elementType.toClass();
		return supportsMimeType(mimeType) && (Object.class == clazz ||
				(!String.class.isAssignableFrom(elementType.resolve(clazz)) && getObjectMapper().canSerialize(clazz)));
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");

		JsonEncoding encoding = getJsonEncoding(mimeType);

		if (inputStream instanceof Mono) {
			return Flux.from(inputStream).map(value ->
					encodeValue(value, mimeType, bufferFactory, elementType, hints, encoding));
		}

		for (MediaType streamingMediaType : this.streamingMediaTypes) {
			if (streamingMediaType.isCompatibleWith(mimeType)) {
				byte[] separator = STREAM_SEPARATORS.getOrDefault(streamingMediaType, NEWLINE_SEPARATOR);
				return Flux.from(inputStream).map(value -> {
					DataBuffer buffer = encodeValue(value, mimeType, bufferFactory, elementType, hints, encoding);
					if (separator != null) {
						buffer.write(separator);
					}
					return buffer;
				});
			}
		}

		ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, elementType);
		return Flux.from(inputStream).collectList().map(list ->
				encodeValue(list, mimeType, bufferFactory, listType, hints, encoding)).flux();
	}

	private DataBuffer encodeValue(Object value, @Nullable MimeType mimeType, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable Map<String, Object> hints, JsonEncoding encoding) {

		if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
			logger.debug(Hints.getLogPrefix(hints) + "Encoding [" + value + "]");
		}

		JavaType javaType = getJavaType(elementType.getType(), null);
		Class<?> jsonView = (hints != null ? (Class<?>) hints.get(Jackson2CodecSupport.JSON_VIEW_HINT) : null);
		ObjectWriter writer = (jsonView != null ?
				getObjectMapper().writerWithView(jsonView) : getObjectMapper().writer());

		if (javaType.isContainerType()) {
			writer = writer.forType(javaType);
		}

		writer = customizeWriter(writer, mimeType, elementType, hints);

		DataBuffer buffer = bufferFactory.allocateBuffer();
		OutputStream outputStream = buffer.asOutputStream();

		try {
			JsonGenerator generator = getObjectMapper().getFactory().createGenerator(outputStream, encoding);
			writer.writeValue(generator, value);
		}
		catch (InvalidDefinitionException ex) {
			throw new CodecException("Type definition error: " + ex.getType(), ex);
		}
		catch (JsonProcessingException ex) {
			throw new EncodingException("JSON encoding error: " + ex.getOriginalMessage(), ex);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unexpected I/O error while writing to data buffer", ex);
		}

		return buffer;
	}

	protected ObjectWriter customizeWriter(ObjectWriter writer, @Nullable MimeType mimeType,
			ResolvableType elementType, @Nullable Map<String, Object> hints) {

		return writer;
	}

	/**
	 * Determine the JSON encoding to use for the given mime type.
	 * @param mimeType the mime type as requested by the caller
	 * @return the JSON encoding to use (never {@code null})
	 * @since 5.0.5
	 */
	protected JsonEncoding getJsonEncoding(@Nullable MimeType mimeType) {
		if (mimeType != null && mimeType.getCharset() != null) {
			Charset charset = mimeType.getCharset();
			for (JsonEncoding encoding : JsonEncoding.values()) {
				if (charset.name().equals(encoding.getJavaName())) {
					return encoding;
				}
			}
		}
		return JsonEncoding.UTF8;
	}


	// HttpMessageEncoder...

	@Override
	public List<MimeType> getEncodableMimeTypes() {
		return getMimeTypes();
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

	// Jackson2CodecSupport ...

	@Override
	protected <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType) {
		return parameter.getMethodAnnotation(annotType);
	}

}
