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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.rsocket.Payload;
import io.rsocket.metadata.WellKnownMimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.reactive.AbstractEncoderMethodReturnValueHandler;
import org.springframework.messaging.rsocket.MetadataEncoder;
import org.springframework.messaging.rsocket.PayloadUtils;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;


/**
 * Extension of {@link AbstractEncoderMethodReturnValueHandler} that
 * {@link #handleEncodedContent handles} encoded content by wrapping data buffers
 * as RSocket payloads and by passing those through the {@link #RESPONSE_HEADER}
 * header.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RSocketPayloadReturnValueHandler extends AbstractEncoderMethodReturnValueHandler {

	/**
	 * Message header name that is expected to have an {@link java.util.concurrent.atomic.AtomicReference}
	 * which will receive the {@code Flux<Payload>} that represents the response.
	 */
	public static final String RESPONSE_HEADER = "rsocketResponse";

	private RSocketStrategies strategies;
	private MimeType compositeMime =  MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());

	public RSocketPayloadReturnValueHandler(List<Encoder<?>> encoders, ReactiveAdapterRegistry registry) {
		super(encoders, registry);
	}
	public RSocketPayloadReturnValueHandler(RSocketStrategies strategies, ReactiveAdapterRegistry registry) {
		this(strategies.encoders(), registry);
		this.strategies = strategies;
	}


	@Override
	protected Mono<Void> handleEncodedContent(
			Flux<DataBuffer> encodedContent, MethodParameter returnType, Message<?> message) {

		AtomicReference<Flux<Payload>> responseRef = getResponseReference(message);
		AtomicReference<MimeType> contentTypeAtomic = getResponseContentType(message);
		Assert.notNull(responseRef, "Missing '" + RESPONSE_HEADER + "'");
		responseRef.set(encodedContent.switchOnFirst((signal, inner) -> {
			DataBuffer data = signal.get();
			if (data != null && contentTypeAtomic.get() != null ) {
				return firstPayload(Mono.fromCallable(() -> data), metadata(contentTypeAtomic.get()))
						.concatWith(inner.skip(1).map(PayloadUtils::createPayload));
			}
			else {
				return inner.map(PayloadUtils::createPayload);
			}
		}));
		return Mono.empty();
	}

	/**
	 * Encode content mime type.
	 **/
	public Mono<DataBuffer> metadata(MimeType contentMimeType) {
			MetadataEncoder metadataEncoder = new MetadataEncoder(this.compositeMime, this.strategies);
			metadataEncoder.metadata(contentMimeType.toString(), MimeType.valueOf(
					WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getString()));
			return metadataEncoder.encode();
	}

	/**
	 * Composite content with metadata into first payload.
	 **/
	private Mono<Payload> firstPayload(Mono<DataBuffer> encodedData,Mono<DataBuffer> metadata) {
		return Mono.zip(encodedData, metadata)
				.map(tuple -> PayloadUtils.createPayload(tuple.getT1(), tuple.getT2()));
	}

	@Override
	protected Mono<Void> handleNoContent(MethodParameter returnType, Message<?> message) {
		AtomicReference<Flux<Payload>> responseRef = getResponseReference(message);
		if (responseRef != null) {
			responseRef.set(Flux.empty());
		}
		return Mono.empty();
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private AtomicReference<Flux<Payload>> getResponseReference(Message<?> message) {
		Object headerValue = message.getHeaders().get(RESPONSE_HEADER);
		Assert.state(headerValue == null || headerValue instanceof AtomicReference, "Expected AtomicReference");
		return (AtomicReference<Flux<Payload>>) headerValue;
	}


}
