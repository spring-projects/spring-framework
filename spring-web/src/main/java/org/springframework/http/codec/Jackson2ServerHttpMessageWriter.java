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

package org.springframework.http.codec;


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonView;
import org.reactivestreams.Publisher;
import static org.springframework.core.codec.AbstractEncoder.FLUSHING_STRATEGY_HINT;
import static org.springframework.core.codec.AbstractEncoder.FlushingStrategy.AFTER_EACH_ELEMENT;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.json.AbstractJackson2Codec;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * Jackson {@link ServerHttpMessageWriter} that resolves {@code @JsonView} annotated handler
 * method and deals with {@link AbstractEncoder#FLUSHING_STRATEGY_HINT}.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see com.fasterxml.jackson.annotation.JsonView
 */
public class Jackson2ServerHttpMessageWriter extends AbstractServerHttpMessageWriter<Object> {


	public Jackson2ServerHttpMessageWriter(Encoder<Object> encoder) {
		super(new EncoderHttpMessageWriter<>(encoder));
	}

	public Jackson2ServerHttpMessageWriter(HttpMessageWriter<Object> writer) {
		super(writer);
	}


	@Override
	protected Map<String, Object> resolveWriteHints(ResolvableType streamType,
			ResolvableType elementType, MediaType mediaType, ServerHttpRequest request) {

		Map<String, Object> hints = new HashMap<>();
		Object source = streamType.getSource();
		MethodParameter returnValue = (source instanceof MethodParameter ? (MethodParameter)source : null);
		if (returnValue != null) {
			JsonView annotation = returnValue.getMethodAnnotation(JsonView.class);
			if (annotation != null) {
				Class<?>[] classes = annotation.value();
				if (classes.length != 1) {
					throw new IllegalArgumentException(
							"@JsonView only supported for write hints with exactly 1 class argument: " + returnValue);
				}
				hints.put(AbstractJackson2Codec.JSON_VIEW_HINT, classes[0]);
			}
		}
		return hints;
	}

	@Override
	public Mono<Void> write(Publisher<?> inputStream, ResolvableType elementType, MediaType mediaType,
			ReactiveHttpOutputMessage outputMessage, Map<String, Object> hints) {

		if ((mediaType != null) && mediaType.isCompatibleWith(MediaType.APPLICATION_STREAM_JSON)) {
			Map<String, Object> hintsWithFlush =  new HashMap<>(hints);
			hintsWithFlush.put(FLUSHING_STRATEGY_HINT, AFTER_EACH_ELEMENT);
			return super.write(inputStream, elementType, mediaType, outputMessage, hintsWithFlush);
		}
		return super.write(inputStream, elementType, mediaType, outputMessage, hints);
	}

	@Override
	public Mono<Void> write(Publisher<?> inputStream, ResolvableType streamType, ResolvableType elementType,
			MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {

		if ((mediaType != null) && mediaType.isCompatibleWith(MediaType.APPLICATION_STREAM_JSON)) {
			Map<String, Object> hintsWithFlush =  new HashMap<>(hints);
			hintsWithFlush.put(FLUSHING_STRATEGY_HINT, AFTER_EACH_ELEMENT);
			return super.write(inputStream, streamType, elementType, mediaType, request, response, hintsWithFlush);
		}
		return super.write(inputStream, streamType, elementType, mediaType, request, response, hints);
	}
}
