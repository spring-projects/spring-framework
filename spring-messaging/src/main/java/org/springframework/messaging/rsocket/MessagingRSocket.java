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

import io.rsocket.AbstractRSocket;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
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
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

/**
 * Package private implementation of {@link RSocket} that is is hooked into an
 * RSocket client or server via {@link MessagingAcceptor} to accept and handle
 * requests.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
class MessagingRSocket extends AbstractRSocket {

	private final ReactiveMessageChannel messageChannel;

	private final RSocketRequester requester;

	@Nullable
	private MimeType dataMimeType;

	private final RSocketStrategies strategies;


	MessagingRSocket(ReactiveMessageChannel messageChannel,
			RSocket sendingRSocket, @Nullable MimeType defaultDataMimeType, RSocketStrategies strategies) {

		Assert.notNull(messageChannel, "'messageChannel' is required");
		Assert.notNull(sendingRSocket, "'sendingRSocket' is required");
		this.messageChannel = messageChannel;
		this.requester = RSocketRequester.create(sendingRSocket, defaultDataMimeType, strategies);
		this.dataMimeType = defaultDataMimeType;
		this.strategies = strategies;
	}



	public Mono<Void> handleConnectionSetupPayload(ConnectionSetupPayload payload) {
		if (StringUtils.hasText(payload.dataMimeType())) {
			this.dataMimeType = MimeTypeUtils.parseMimeType(payload.dataMimeType());
		}
		return handle(payload);
	}


	@Override
	public Mono<Void> fireAndForget(Payload payload) {
		return handle(payload);
	}

	@Override
	public Mono<Payload> requestResponse(Payload payload) {
		return handleAndReply(payload, Flux.just(payload)).next();
	}

	@Override
	public Flux<Payload> requestStream(Payload payload) {
		return handleAndReply(payload, Flux.just(payload));
	}

	@Override
	public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
		return Flux.from(payloads)
				.switchOnFirst((signal, innerFlux) -> {
					Payload firstPayload = signal.get();
					return firstPayload == null ? innerFlux : handleAndReply(firstPayload, innerFlux);
				});
	}

	@Override
	public Mono<Void> metadataPush(Payload payload) {
		// This won't be very useful until createHeaders starting doing something more with metadata..
		return handle(payload);
	}


	private Mono<Void> handle(Payload payload) {

		Message<?> message = MessageBuilder.createMessage(
				Mono.fromCallable(() -> wrapPayloadData(payload)),
				createHeaders(payload, null));

		return this.messageChannel.send(message).flatMap(result -> result ?
				Mono.empty() : Mono.error(new MessageDeliveryException("RSocket request not handled")));
	}

	private Flux<Payload> handleAndReply(Payload firstPayload, Flux<Payload> payloads) {

		MonoProcessor<Flux<Payload>> replyMono = MonoProcessor.create();

		Message<?> message = MessageBuilder.createMessage(
				payloads.map(this::wrapPayloadData).doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release),
				createHeaders(firstPayload, replyMono));

		return this.messageChannel.send(message).flatMapMany(result ->
				result && replyMono.isTerminated() ? replyMono.flatMapMany(Function.identity()) :
						Mono.error(new MessageDeliveryException("RSocket request not handled")));
	}

	private MessageHeaders createHeaders(Payload payload, @Nullable MonoProcessor<?> replyMono) {

		// TODO:
		// For now treat the metadata as a simple string with routing information.
		// We'll have to get more sophisticated once the routing extension is completed.
		// https://github.com/rsocket/rsocket-java/issues/568

		MessageHeaderAccessor headers = new MessageHeaderAccessor();

		String destination = payload.getMetadataUtf8();
		headers.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, destination);

		if (this.dataMimeType != null) {
			headers.setContentType(this.dataMimeType);
		}

		headers.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, this.requester);

		if (replyMono != null) {
			headers.setHeader(RSocketPayloadReturnValueHandler.RESPONSE_HEADER, replyMono);
		}

		DataBufferFactory bufferFactory = this.strategies.dataBufferFactory();
		headers.setHeader(HandlerMethodReturnValueHandler.DATA_BUFFER_FACTORY_HEADER, bufferFactory);

		return headers.getMessageHeaders();
	}

	private DataBuffer wrapPayloadData(Payload payload) {
		return PayloadUtils.wrapPayloadData(payload, this.strategies.dataBufferFactory());
	}

}
