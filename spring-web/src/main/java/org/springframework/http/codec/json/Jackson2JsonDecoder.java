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

package org.springframework.http.codec.json;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.HttpMessageDecoder;
import org.springframework.core.codec.InternalCodecException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Decode a byte stream into JSON and convert to Object's with Jackson 2.9.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see Jackson2JsonEncoder
 */
public class Jackson2JsonDecoder extends Jackson2CodecSupport implements HttpMessageDecoder<Object> {

	private final JsonObjectDecoder fluxDecoder = new JsonObjectDecoder(true);

	private final JsonObjectDecoder monoDecoder = new JsonObjectDecoder(false);


	public Jackson2JsonDecoder() {
		super(Jackson2ObjectMapperBuilder.json().build());
	}

	public Jackson2JsonDecoder(ObjectMapper mapper) {
		super(mapper);
	}


	@Override
	public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
		JavaType javaType = this.objectMapper.getTypeFactory().constructType(elementType.getType());
		// Skip String (CharSequenceDecoder + "*/*" comes after)
		return (!CharSequence.class.isAssignableFrom(elementType.resolve(Object.class)) &&
				this.objectMapper.canDeserialize(javaType) && supportsMimeType(mimeType));
	}


	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return JSON_MIME_TYPES;
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			MimeType mimeType, Map<String, Object> hints) {

		return decodeInternal(this.fluxDecoder, input, elementType, mimeType, hints);
	}

	@Override
	public Mono<Object> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
			MimeType mimeType, Map<String, Object> hints) {

		return decodeInternal(this.monoDecoder, input, elementType, mimeType, hints).singleOrEmpty();
	}

	private Flux<Object> decodeInternal(JsonObjectDecoder objectDecoder, Publisher<DataBuffer> inputStream,
			ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {

		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");

		Class<?> contextClass = getParameter(elementType).map(MethodParameter::getContainingClass).orElse(null);
		JavaType javaType = getJavaType(elementType.getType(), contextClass);
		Class<?> jsonView = (Class<?>) hints.get(Jackson2CodecSupport.JSON_VIEW_HINT);

		ObjectReader reader = (jsonView != null ?
				this.objectMapper.readerWithView(jsonView).forType(javaType) :
				this.objectMapper.readerFor(javaType));

		return objectDecoder.decode(inputStream, elementType, mimeType, hints)
				.map(dataBuffer -> {
					try {
						Object value = reader.readValue(dataBuffer.asInputStream());
						DataBufferUtils.release(dataBuffer);
						return value;
					}
					catch (InvalidDefinitionException ex) {
						throw new InternalCodecException("Error while reading the data", ex);
					}
					catch (IOException ex) {
						throw new CodecException("Error while reading the data", ex);
					}
				});
	}


	// HttpMessageDecoder...

	@Override
	public Map<String, Object> getDecodeHints(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response) {

		return getHints(actualType);
	}

	@Override
	protected <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType) {
		return parameter.getParameterAnnotation(annotType);
	}

}
