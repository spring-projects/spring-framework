/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Decoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;


/**
 * Decode a byte stream into JSON and convert to Object's with Jackson.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see JacksonJsonEncoder
 */
public class JacksonJsonDecoder extends AbstractJacksonJsonCodec implements Decoder<Object> {

	private final JsonObjectDecoder fluxObjectDecoder = new JsonObjectDecoder(true);

	private final JsonObjectDecoder monoObjectDecoder = new JsonObjectDecoder(false);


	public JacksonJsonDecoder() {
		super(Jackson2ObjectMapperBuilder.json().build());
	}

	public JacksonJsonDecoder(ObjectMapper mapper) {
		super(mapper);
	}

	@Override
	public boolean canDecode(ResolvableType elementType, MimeType mimeType, Object... hints) {
		if (mimeType == null) {
			return true;
		}
		return JSON_MIME_TYPES.stream().anyMatch(m -> m.isCompatibleWith(mimeType));
	}

	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return JSON_MIME_TYPES;
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			MimeType mimeType, Object... hints) {

		JsonObjectDecoder objectDecoder = this.fluxObjectDecoder;
		return decodeInternal(objectDecoder, inputStream, elementType, mimeType, hints);
	}

	@Override
	public Mono<Object> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			MimeType mimeType, Object... hints) {

		JsonObjectDecoder objectDecoder = this.monoObjectDecoder;
		return decodeInternal(objectDecoder, inputStream, elementType, mimeType, hints).single();
	}

	private Flux<Object> decodeInternal(JsonObjectDecoder objectDecoder, Publisher<DataBuffer> inputStream,
			ResolvableType elementType, MimeType mimeType, Object[] hints) {

		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");

		MethodParameter methodParameter = (elementType.getSource() instanceof MethodParameter ?
				(MethodParameter)elementType.getSource() : null);
		Class<?> contextClass = (methodParameter != null ? methodParameter.getContainingClass() : null);
		JavaType javaType = getJavaType(elementType.getType(), contextClass);
		ObjectReader reader;

		if (methodParameter != null && methodParameter.getParameter().getAnnotation(JsonView.class) != null) {
			JsonView annotation = methodParameter.getParameter().getAnnotation(JsonView.class);
			Class<?>[] classes = annotation.value();
			if (classes.length != 1) {
				throw new IllegalArgumentException(
						"@JsonView only supported for response body advice with exactly 1 class argument: " + methodParameter);
			}
			reader = mapper.readerWithView(classes[0]).forType(javaType);
		}
		else {
			reader = mapper.readerFor(javaType);
		}

		return objectDecoder.decode(inputStream, elementType, mimeType, hints)
				.map(dataBuffer -> {
					try {
						Object value = reader.readValue(dataBuffer.asInputStream());
						DataBufferUtils.release(dataBuffer);
						return value;
					}
					catch (IOException e) {
						return Flux.error(new CodecException("Error while reading the data", e));
					}
				});
	}

}
