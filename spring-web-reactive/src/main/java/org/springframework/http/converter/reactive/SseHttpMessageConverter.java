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

package org.springframework.http.converter.reactive;


import java.util.Arrays;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.SseEventEncoder;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.web.reactive.sse.SseEvent;

/**
 * Implementation of {@link HttpMessageConverter} that can stream Server-Sent Events
 * response.
 *
 * It allows to write {@code Flux<ServerSentEvent>}, which is Spring Web Reactive equivalent
 * to Spring MVC {@code SseEmitter}.
 *
 * Sending {@code Flux<String>} or {@code Flux<Pojo>} is equivalent to sending
 * {@code Flux<SseEvent>} with the {@code data} property set to the {@code String} or
 * {@code Pojo} value.
 *
 * @author Sebastien Deleuze
 * @see SseEvent
 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommandation</a>
 */
public class SseHttpMessageConverter extends CodecHttpMessageConverter<Object> {

	/**
	 * Constructor that creates a new instance configured with the specified data encoders.
	 */
	public SseHttpMessageConverter(List<Encoder<?>> dataEncoders) {
		super(new SseEventEncoder(dataEncoders), null);
	}

	@Override
	public Mono<Void> write(Publisher<?> inputStream, ResolvableType type,
			MediaType contentType, ReactiveHttpOutputMessage outputMessage) {

		outputMessage.getHeaders().add("Content-Type", "text/event-stream");
		// Keep the SSE connection open even for cold stream in order to avoid unexpected Browser reconnection
		return super.write(Flux.from(inputStream).concatWith(Flux.never()), type, contentType, outputMessage);
	}

}
