/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.messaging.rsocket;

import java.util.function.Function;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.ReactiveMessageChannel;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Package private implementation of {@link RSocket} used from
 * {@link MessagingAcceptor}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
class MessagingRSocket implements RSocket {

	private final ReactiveMessageChannel messageChannel;

	private final NettyDataBufferFactory bufferFactory;

	private final RSocket sendingRSocket;

	@Nullable
	private final MimeType dataMimeType;


	MessagingRSocket(ReactiveMessageChannel messageChannel, NettyDataBufferFactory bufferFactory,
			RSocket sendingRSocket, @Nullable MimeType dataMimeType) {

		Assert.notNull(messageChannel, "'messageChannel' is required");
		Assert.notNull(bufferFactory, "'bufferFactory' is required");
		Assert.notNull(sendingRSocket, "'sendingRSocket' is required");
		this.messageChannel = messageChannel;
		this.bufferFactory = bufferFactory;
		this.sendingRSocket = sendingRSocket;
		this.dataMimeType = dataMimeType;
	}


	public Mono<Void> afterConnectionEstablished(ConnectionSetupPayload payload) {
		return execute(payload).flatMap(flux -> flux.take(0).then());
	}


	@Override
	public Mono<Void> fireAndForget(Payload payload) {
		return execute(payload).flatMap(flux -> flux.take(0).then());
	}

	@Override
	public Mono<Payload> requestResponse(Payload payload) {
		return execute(payload).flatMap(Flux::next);
	}

	@Override
	public Flux<Payload> requestStream(Payload payload) {
		return execute(payload).flatMapMany(Function.identity());
	}

	@Override
	public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
		return Flux.from(payloads)
				.switchOnFirst((signal, inner) -> {
					Payload first = signal.get();
					return first != null ? execute(first, inner).flatMapMany(Function.identity()) : inner;
				});
	}

	@Override
	public Mono<Void> metadataPush(Payload payload) {
		return null;
	}

	private Mono<Flux<Payload>> execute(Payload payload) {
		return execute(payload, Flux.just(payload));
	}

	private Mono<Flux<Payload>> execute(Payload firstPayload, Flux<Payload> payloads) {

		// TODO:
		// Since we do retain(), we need to ensure buffers are released if not consumed,
		// e.g. error before Flux subscribed to, no handler found, @MessageMapping ignores payload, etc.

		Flux<NettyDataBuffer> payloadDataBuffers = payloads
				.map(payload -> this.bufferFactory.wrap(payload.retain().sliceData()))
				.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);

		MonoProcessor<Flux<Payload>> replyMono = MonoProcessor.create();
		MessageHeaders headers = createHeaders(firstPayload, replyMono);

		Message<?> message = MessageBuilder.createMessage(payloadDataBuffers, headers);

		return this.messageChannel.send(message).flatMap(result -> result ?
				replyMono.isTerminated() ? replyMono : Mono.empty() :
				Mono.error(new MessageDeliveryException("RSocket interaction not handled")));
	}

	private MessageHeaders createHeaders(Payload payload, MonoProcessor<?> replyMono) {

		// For now treat the metadata as a simple string with routing information.
		// We'll have to get more sophisticated once the routing extension is completed.
		// https://github.com/rsocket/rsocket-java/issues/568

		MessageHeaderAccessor headers = new MessageHeaderAccessor();

		String destination = payload.getMetadataUtf8();
		headers.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, destination);

		if (this.dataMimeType != null) {
			headers.setContentType(this.dataMimeType);
		}

		headers.setHeader(SendingRSocketMethodArgumentResolver.SENDING_RSOCKET_HEADER, this.sendingRSocket);
		headers.setHeader(RSocketPayloadReturnValueHandler.RESPONSE_HEADER, replyMono);
		headers.setHeader(HandlerMethodReturnValueHandler.DATA_BUFFER_FACTORY_HEADER, this.bufferFactory);

		return headers.getMessageHeaders();
	}

	@Override
	public Mono<Void> onClose() {
		return null;
	}

	@Override
	public void dispose() {
	}

}
