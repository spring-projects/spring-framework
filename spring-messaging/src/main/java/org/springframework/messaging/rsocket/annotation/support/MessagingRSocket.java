/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.rsocket.AbstractRSocket;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.frame.FrameType;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.PayloadUtils;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.RouteMatcher;

/**
 * Responder {@link RSocket} that wraps the payload and metadata of incoming
 * requests as a {@link Message} and then delegates to the configured
 * {@link RSocketMessageHandler} to handle it. The response, if applicable, is
 * obtained from the {@link RSocketPayloadReturnValueHandler#RESPONSE_HEADER
 * "rsocketResponse"} header.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
class MessagingRSocket extends AbstractRSocket {

	private final MimeType dataMimeType;

	private final MimeType metadataMimeType;

	private final MetadataExtractor metadataExtractor;

	private final ReactiveMessageHandler messageHandler;

	private final RouteMatcher routeMatcher;

	private final RSocketRequester requester;

	private final RSocketStrategies strategies;


	MessagingRSocket(MimeType dataMimeType, MimeType metadataMimeType, MetadataExtractor metadataExtractor,
			RSocketRequester requester, ReactiveMessageHandler messageHandler, RouteMatcher routeMatcher,
			RSocketStrategies strategies) {

		Assert.notNull(dataMimeType, "'dataMimeType' is required");
		Assert.notNull(metadataMimeType, "'metadataMimeType' is required");
		Assert.notNull(metadataExtractor, "MetadataExtractor is required");
		Assert.notNull(requester, "RSocketRequester is required");
		Assert.notNull(messageHandler, "ReactiveMessageHandler is required");
		Assert.notNull(routeMatcher, "RouteMatcher is required");
		Assert.notNull(strategies, "RSocketStrategies is required");

		this.dataMimeType = dataMimeType;
		this.metadataMimeType = metadataMimeType;
		this.metadataExtractor = metadataExtractor;
		this.requester = requester;
		this.messageHandler = messageHandler;
		this.routeMatcher = routeMatcher;
		this.strategies = strategies;
	}


	/**
	 * Wrap the {@link ConnectionSetupPayload} with a {@link Message} and
	 * delegate to {@link #handle(Payload, FrameType)} for handling.
	 * @param payload the connection payload
	 * @return completion handle for success or error
	 */
	public Mono<Void> handleConnectionSetupPayload(ConnectionSetupPayload payload) {
		// frameDecoder does not apply to connectionSetupPayload
		// so retain here since handle expects it..
		payload.retain();
		return handle(payload, FrameType.SETUP);
	}


	@Override
	public Mono<Void> fireAndForget(Payload payload) {
		return handle(payload, FrameType.REQUEST_FNF);
	}

	@Override
	public Mono<Payload> requestResponse(Payload payload) {
		return handleAndReply(payload, FrameType.REQUEST_RESPONSE, Flux.just(payload)).next();
	}

	@Override
	public Flux<Payload> requestStream(Payload payload) {
		return handleAndReply(payload, FrameType.REQUEST_STREAM, Flux.just(payload));
	}

	@Override
	public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
		return Flux.from(payloads)
				.switchOnFirst((signal, innerFlux) -> {
					Payload firstPayload = signal.get();
					return firstPayload == null ? innerFlux :
							handleAndReply(firstPayload, FrameType.REQUEST_CHANNEL, innerFlux);
				});
	}

	@Override
	public Mono<Void> metadataPush(Payload payload) {
		// Not very useful until createHeaders does more with metadata
		return handle(payload, FrameType.METADATA_PUSH);
	}


	private Mono<Void> handle(Payload payload, FrameType frameType) {
		MessageHeaders headers = createHeaders(payload, frameType, null);
		DataBuffer dataBuffer = retainDataAndReleasePayload(payload);
		int refCount = refCount(dataBuffer);
		Message<?> message = MessageBuilder.createMessage(dataBuffer, headers);
		return Mono.defer(() -> this.messageHandler.handleMessage(message))
				.doFinally(s -> {
					if (refCount(dataBuffer) == refCount) {
						DataBufferUtils.release(dataBuffer);
					}
				});
	}

	private int refCount(DataBuffer dataBuffer) {
		return dataBuffer instanceof NettyDataBuffer ?
				((NettyDataBuffer) dataBuffer).getNativeBuffer().refCnt() : 1;
	}

	private Flux<Payload> handleAndReply(Payload firstPayload, FrameType frameType, Flux<Payload> payloads) {
		MonoProcessor<Flux<Payload>> replyMono = MonoProcessor.create();
		MessageHeaders headers = createHeaders(firstPayload, frameType, replyMono);

		AtomicBoolean read = new AtomicBoolean();
		Flux<DataBuffer> buffers = payloads.map(this::retainDataAndReleasePayload).doOnSubscribe(s -> read.set(true));
		Message<Flux<DataBuffer>> message = MessageBuilder.createMessage(buffers, headers);

		return Mono.defer(() -> this.messageHandler.handleMessage(message))
				.doFinally(s -> {
					// Subscription should have happened by now due to ChannelSendOperator
					if (!read.get()) {
						firstPayload.release();
					}
				})
				.thenMany(Flux.defer(() -> replyMono.isTerminated() ?
						replyMono.flatMapMany(Function.identity()) :
						Mono.error(new IllegalStateException("Something went wrong: reply Mono not set"))));
	}

	private DataBuffer retainDataAndReleasePayload(Payload payload) {
		return PayloadUtils.retainDataAndReleasePayload(payload, this.strategies.dataBufferFactory());
	}

	private MessageHeaders createHeaders(Payload payload, FrameType frameType,
			@Nullable MonoProcessor<?> replyMono) {

		MessageHeaderAccessor headers = new MessageHeaderAccessor();
		headers.setLeaveMutable(true);

		Map<String, Object> metadataValues = this.metadataExtractor.extract(payload, this.metadataMimeType);

		metadataValues.putIfAbsent(MetadataExtractor.ROUTE_KEY, "");
		for (Map.Entry<String, Object> entry : metadataValues.entrySet()) {
			if (entry.getKey().equals(MetadataExtractor.ROUTE_KEY)) {
				RouteMatcher.Route route = this.routeMatcher.parseRoute((String) entry.getValue());
				headers.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, route);
			}
			else {
				headers.setHeader(entry.getKey(), entry.getValue());
			}
		}

		headers.setContentType(this.dataMimeType);
		headers.setHeader(RSocketFrameTypeMessageCondition.FRAME_TYPE_HEADER, frameType);
		headers.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, this.requester);
		if (replyMono != null) {
			headers.setHeader(RSocketPayloadReturnValueHandler.RESPONSE_HEADER, replyMono);
		}
		headers.setHeader(HandlerMethodReturnValueHandler.DATA_BUFFER_FACTORY_HEADER,
				this.strategies.dataBufferFactory());

		return headers.getMessageHeaders();
	}

}
