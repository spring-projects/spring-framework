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

package org.springframework.messaging.rsocket;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.rsocket.AbstractRSocket;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.metadata.CompositeMetadata;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
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

	private final RSocketMessageHandler messageHandler;

	private final RouteMatcher routeMatcher;

	private final RSocketRequester requester;

	private final MimeType dataMimeType;

	private final MimeType metadataMimeType;

	private final DataBufferFactory bufferFactory;


	MessagingRSocket(RSocketMessageHandler messageHandler, RouteMatcher routeMatcher,
			RSocketRequester requester, MimeType dataMimeType, MimeType metadataMimeType,
			DataBufferFactory bufferFactory) {

		Assert.notNull(messageHandler, "'messageHandler' is required");
		Assert.notNull(routeMatcher, "'routeMatcher' is required");
		Assert.notNull(requester, "'requester' is required");
		Assert.notNull(requester, "'dataMimeType' is required");
		Assert.notNull(requester, "'metadataMimeType' is required");

		Assert.isTrue(DefaultRSocketRequester.METADATA_MIME_TYPES.contains(metadataMimeType),
				() -> "Unexpected metadatata mime type: '" + metadataMimeType + "'");

		this.messageHandler = messageHandler;
		this.routeMatcher = routeMatcher;
		this.requester = requester;
		this.dataMimeType = dataMimeType;
		this.metadataMimeType = metadataMimeType;
		this.bufferFactory = bufferFactory;
	}


	/**
	 * Wrap the {@link ConnectionSetupPayload} with a {@link Message} and
	 * delegate to {@link #handle(Payload)} for handling.
	 * @param payload the connection payload
	 * @return completion handle for success or error
	 */
	public Mono<Void> handleConnectionSetupPayload(ConnectionSetupPayload payload) {
		// frameDecoder does not apply to connectionSetupPayload
		// so retain here since handle expects it..
		payload.retain();
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
		// Not very useful until createHeaders does more with metadata
		return handle(payload);
	}


	private Mono<Void> handle(Payload payload) {
		String destination = getDestination(payload);
		MessageHeaders headers = createHeaders(destination, null);
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

	private Flux<Payload> handleAndReply(Payload firstPayload, Flux<Payload> payloads) {
		MonoProcessor<Flux<Payload>> replyMono = MonoProcessor.create();
		String destination = getDestination(firstPayload);
		MessageHeaders headers = createHeaders(destination, replyMono);

		AtomicBoolean read = new AtomicBoolean();
		Flux<DataBuffer> buffers = payloads.map(this::retainDataAndReleasePayload).doOnSubscribe(s -> read.set(true));
		Message<Flux<DataBuffer>> message = MessageBuilder.createMessage(buffers, headers);

		return Mono.defer(() -> this.messageHandler.handleMessage(message))
				.doFinally(s -> {
					// Subscription should have happened by now due to ChannelSendOperator
					if (!read.get()) {
						buffers.subscribe(DataBufferUtils::release);
					}
				})
				.thenMany(Flux.defer(() -> replyMono.isTerminated() ?
						replyMono.flatMapMany(Function.identity()) :
						Mono.error(new IllegalStateException("Something went wrong: reply Mono not set"))));
	}

	private String getDestination(Payload payload) {
		if (this.metadataMimeType.equals(DefaultRSocketRequester.COMPOSITE_METADATA)) {
			CompositeMetadata metadata = new CompositeMetadata(payload.metadata(), false);
			for (CompositeMetadata.Entry entry : metadata) {
				String mimeType = entry.getMimeType();
				if (DefaultRSocketRequester.ROUTING.toString().equals(mimeType)) {
					return entry.getContent().toString(StandardCharsets.UTF_8);
				}
			}
			return "";
		}
		else if (this.metadataMimeType.equals(DefaultRSocketRequester.ROUTING)) {
			return payload.getMetadataUtf8();
		}
		// Should not happen (given constructor assertions)
		throw new IllegalArgumentException("Unexpected metadata MimeType");
	}

	private DataBuffer retainDataAndReleasePayload(Payload payload) {
		return PayloadUtils.retainDataAndReleasePayload(payload, this.bufferFactory);
	}

	private MessageHeaders createHeaders(String destination, @Nullable MonoProcessor<?> replyMono) {
		MessageHeaderAccessor headers = new MessageHeaderAccessor();
		headers.setLeaveMutable(true);
		RouteMatcher.Route route = this.routeMatcher.parseRoute(destination);
		headers.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, route);
		headers.setContentType(this.dataMimeType);
		headers.setHeader(RSocketRequesterMethodArgumentResolver.RSOCKET_REQUESTER_HEADER, this.requester);
		if (replyMono != null) {
			headers.setHeader(RSocketPayloadReturnValueHandler.RESPONSE_HEADER, replyMono);
		}
		headers.setHeader(HandlerMethodReturnValueHandler.DATA_BUFFER_FACTORY_HEADER, this.bufferFactory);
		return headers.getMessageHeaders();
	}

}
