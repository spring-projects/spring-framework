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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerHttpEncoder;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects,
 * using Jackson 2.6+.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 * @see Jackson2JsonDecoder
 */
public class Jackson2JsonEncoder extends Jackson2CodecSupport implements ServerHttpEncoder<Object> {

	private final PrettyPrinter ssePrettyPrinter;


	public Jackson2JsonEncoder() {
		this(Jackson2ObjectMapperBuilder.json().build());
	}

	public Jackson2JsonEncoder(ObjectMapper mapper) {
		super(mapper);
		DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
		prettyPrinter.indentObjectsWith(new DefaultIndenter("  ", "\ndata:"));
		this.ssePrettyPrinter = prettyPrinter;
	}


	@Override
	public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
		return this.mapper.canSerialize(elementType.getRawClass()) &&
				(mimeType == null || JSON_MIME_TYPES.stream().anyMatch(m -> m.isCompatibleWith(mimeType)));
	}

	@Override
	public List<MimeType> getEncodableMimeTypes() {
		return JSON_MIME_TYPES;
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {

		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");

		if (inputStream instanceof Mono) {
			return Flux.from(inputStream).map(value -> encodeValue(value, bufferFactory, elementType, hints));
		}
		else if (APPLICATION_STREAM_JSON.isCompatibleWith(mimeType)) {
			return Flux.from(inputStream).map(value -> {
				DataBuffer buffer = encodeValue(value, bufferFactory, elementType, hints);
				buffer.write(new byte[]{'\n'});
				return buffer;
			});
		}
		ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, elementType);
		return Flux.from(inputStream).collectList().map(list -> encodeValue(list, bufferFactory, listType, hints)).flux();
	}

	private DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory,
			ResolvableType elementType, Map<String, Object> hints) {

		TypeFactory typeFactory = this.mapper.getTypeFactory();
		JavaType javaType = typeFactory.constructType(elementType.getType());
		if (elementType.isInstance(value)) {
			javaType = getJavaType(elementType.getType(), null);
		}

		Class<?> jsonView = (Class<?>) hints.get(Jackson2CodecSupport.JSON_VIEW_HINT);
		ObjectWriter writer = jsonView != null ? this.mapper.writerWithView(jsonView): this.mapper.writer();

		if (javaType != null && javaType.isContainerType()) {
			writer = writer.forType(javaType);
		}

		SerializationConfig config = writer.getConfig();
		Boolean sseHint = (Boolean) hints.get(ServerSentEventHttpMessageWriter.SSE_CONTENT_HINT);
		if (Boolean.TRUE.equals(sseHint) && config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
			writer = writer.with(this.ssePrettyPrinter);
		}

		DataBuffer buffer = bufferFactory.allocateBuffer();
		OutputStream outputStream = buffer.asOutputStream();
		try {
			writer.writeValue(outputStream, value);
		}
		catch (IOException ex) {
			throw new CodecException("Error while writing the data", ex);
		}

		return buffer;
	}


	// ServerHttpEncoder...

	@Override
	public Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType,
			MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		return getHints(actualType);
	}

	@Override
	protected <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType) {
		return parameter.getMethodAnnotation(annotType);
	}

}
