/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.rsocket.annotation.support;

import java.util.List;

import io.rsocket.Payload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.reactive.AbstractEncoderMethodReturnValueHandler;
import org.springframework.messaging.rsocket.PayloadUtils;
import org.springframework.util.Assert;

/**
 * Extension of {@link AbstractEncoderMethodReturnValueHandler} that
 * {@link #handleEncodedContent handles} encoded content by wrapping data buffers
 * as RSocket payloads and by passing those to the {@link MonoProcessor}
 * from the {@link #RESPONSE_HEADER} header.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RSocketPayloadReturnValueHandler extends AbstractEncoderMethodReturnValueHandler {

	/**
	 * Message header name that is expected to have a {@link MonoProcessor}
	 * which will receive the {@code Flux<Payload>} that represents the response.
	 */
	public static final String RESPONSE_HEADER = "rsocketResponse";


	public RSocketPayloadReturnValueHandler(List<Encoder<?>> encoders, ReactiveAdapterRegistry registry) {
		super(encoders, registry);
	}


	@Override
	@SuppressWarnings("unchecked")
	protected Mono<Void> handleEncodedContent(
			Flux<DataBuffer> encodedContent, MethodParameter returnType, Message<?> message) {

		MonoProcessor<Flux<Payload>> replyMono = getReplyMono(message);
		Assert.notNull(replyMono, "Missing '" + RESPONSE_HEADER + "'");
		replyMono.onNext(encodedContent.map(PayloadUtils::createPayload));
		replyMono.onComplete();
		return Mono.empty();
	}

	@Override
	protected Mono<Void> handleNoContent(MethodParameter returnType, Message<?> message) {
		MonoProcessor<Flux<Payload>> replyMono = getReplyMono(message);
		if (replyMono != null) {
			replyMono.onComplete();
		}
		return Mono.empty();
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private MonoProcessor<Flux<Payload>> getReplyMono(Message<?> message) {
		Object headerValue = message.getHeaders().get(RESPONSE_HEADER);
		Assert.state(headerValue == null || headerValue instanceof MonoProcessor, "Expected MonoProcessor");
		return (MonoProcessor<Flux<Payload>>) headerValue;
	}

}
