/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;

/**
 * Default implementation of {@link RSocketRequester}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
final class DefaultRSocketRequester implements RSocketRequester {

	private static final Map<String, Object> EMPTY_HINTS = Collections.emptyMap();

	private final RSocketClient rsocketClient;

	private final @Nullable RSocket rsocket;

	private final MimeType dataMimeType;

	private final MimeType metadataMimeType;

	private final RSocketStrategies strategies;

	private final Mono<DataBuffer> emptyBufferMono;


	DefaultRSocketRequester(
			@Nullable RSocketClient rsocketClient, @Nullable RSocket rsocket,
			MimeType dataMimeType, MimeType metadataMimeType, RSocketStrategies strategies) {

		Assert.isTrue(rsocketClient != null || rsocket != null, "RSocketClient or RSocket is required");
		Assert.notNull(dataMimeType, "'dataMimeType' is required");
		Assert.notNull(metadataMimeType, "'metadataMimeType' is required");
		Assert.notNull(strategies, "RSocketStrategies is required");

		this.rsocketClient = (rsocketClient != null ? rsocketClient : RSocketClient.from(rsocket));
		this.rsocket = rsocket;
		this.dataMimeType = dataMimeType;
		this.metadataMimeType = metadataMimeType;
		this.strategies = strategies;
		this.emptyBufferMono = Mono.just(this.strategies.dataBufferFactory().wrap(new byte[0]));
	}


	@Override
	public RSocketClient rsocketClient() {
		return this.rsocketClient;
	}

	@Override
	public @Nullable RSocket rsocket() {
		return this.rsocket;
	}

	@Override
	public MimeType dataMimeType() {
		return this.dataMimeType;
	}

	@Override
	public MimeType metadataMimeType() {
		return this.metadataMimeType;
	}

	@Override
	public RSocketStrategies strategies() {
		return this.strategies;
	}


	@Override
	public RequestSpec route(String route, Object... vars) {
		return new DefaultRequestSpec(route, vars);
	}

	@Override
	public RequestSpec metadata(Object metadata, @Nullable MimeType mimeType) {
		return new DefaultRequestSpec(metadata, mimeType);
	}

	private static boolean isVoid(ResolvableType elementType) {
		return ClassUtils.isVoidType(elementType.resolve());
	}

	private DataBufferFactory bufferFactory() {
		return this.strategies.dataBufferFactory();
	}


	private class DefaultRequestSpec implements RequestSpec {

		private final MetadataEncoder metadataEncoder = new MetadataEncoder(metadataMimeType(), strategies);

		private @Nullable Mono<Payload> payloadMono;

		private @Nullable Flux<Payload> payloadFlux;


		public DefaultRequestSpec(String route, Object... vars) {
			this.metadataEncoder.route(route, vars);
		}

		public DefaultRequestSpec(Object metadata, @Nullable MimeType mimeType) {
			this.metadataEncoder.metadata(metadata, mimeType);
		}


		@Override
		public RequestSpec metadata(Object metadata, MimeType mimeType) {
			this.metadataEncoder.metadata(metadata, mimeType);
			return this;
		}

		@Override
		public RequestSpec metadata(Consumer<MetadataSpec<?>> configurer) {
			configurer.accept(this);
			return this;
		}

		@Override
		public RequestSpec data(Object data) {
			Assert.notNull(data, "'data' must not be null");
			createPayload(data, ResolvableType.NONE);
			return this;
		}

		@Override
		public RequestSpec data(Object producer, Class<?> elementClass) {
			Assert.notNull(producer, "'producer' must not be null");
			Assert.notNull(elementClass, "'elementClass' must not be null");
			ReactiveAdapter adapter = getAdapter(producer.getClass());
			Assert.notNull(adapter, () -> "'producer' type is unknown to ReactiveAdapterRegistry: " +
					producer.getClass().getName());
			createPayload(adapter.toPublisher(producer), ResolvableType.forClass(elementClass));
			return this;
		}

		private @Nullable ReactiveAdapter getAdapter(Class<?> aClass) {
			return strategies.reactiveAdapterRegistry().getAdapter(aClass);
		}

		@Override
		public RequestSpec data(Object producer, ParameterizedTypeReference<?> elementTypeRef) {
			Assert.notNull(producer, "'producer' must not be null");
			Assert.notNull(elementTypeRef, "'elementTypeRef' must not be null");
			ReactiveAdapter adapter = getAdapter(producer.getClass());
			Assert.notNull(adapter, () -> "'producer' type is unknown to ReactiveAdapterRegistry: " +
					producer.getClass().getName());
			createPayload(adapter.toPublisher(producer), ResolvableType.forType(elementTypeRef));
			return this;
		}

		private void createPayload(Object input, ResolvableType elementType) {
			ReactiveAdapter adapter = getAdapter(input.getClass());
			Publisher<?> publisher;
			if (input instanceof Publisher<?> _publisher) {
				publisher = _publisher;
			}
			else if (adapter != null) {
				publisher = adapter.toPublisher(input);
			}
			else {
				ResolvableType type = ResolvableType.forInstance(input);
				this.payloadMono = firstPayload(Mono.fromCallable(() -> encodeData(input, type, null)));
				this.payloadFlux = null;
				return;
			}

			if (isVoid(elementType) || (adapter != null && adapter.isNoValue())) {
				this.payloadMono = Mono.when(publisher).then(firstPayload(emptyBufferMono));
				this.payloadFlux = null;
				return;
			}

			Encoder<?> encoder = elementType != ResolvableType.NONE && !Object.class.equals(elementType.resolve()) ?
					strategies.encoder(elementType, dataMimeType) : null;

			if (adapter != null && !adapter.isMultiValue()) {
				Mono<DataBuffer> data = Mono.from(publisher)
						.map(value -> encodeData(value, elementType, encoder))
						.switchIfEmpty(emptyBufferMono);
				this.payloadMono = firstPayload(data);
				this.payloadFlux = null;
				return;
			}

			this.payloadMono = null;
			this.payloadFlux = Flux.from(publisher)
					.map(value -> encodeData(value, elementType, encoder))
					.switchIfEmpty(emptyBufferMono)
					.switchOnFirst((signal, inner) -> {
						DataBuffer data = signal.get();
						if (data != null) {
							return firstPayload(Mono.fromCallable(() -> data))
									.concatWith(inner.skip(1).map(PayloadUtils::createPayload));
						}
						else {
							return inner.map(PayloadUtils::createPayload);
						}
					})
					.doOnDiscard(Payload.class, Payload::release);
		}

		@SuppressWarnings("unchecked")
		private <T> DataBuffer encodeData(T value, ResolvableType elementType, @Nullable Encoder<?> encoder) {
			if (encoder == null) {
				elementType = ResolvableType.forInstance(value);
				encoder = strategies.encoder(elementType, dataMimeType);
			}
			return ((Encoder<T>) encoder).encodeValue(
					value, bufferFactory(), elementType, dataMimeType, EMPTY_HINTS);
		}

		/**
		 * Create the 1st request payload with encoded data and metadata.
		 * @param encodedData the encoded payload data; expected to not be empty!
		 */
		private Mono<Payload> firstPayload(Mono<DataBuffer> encodedData) {
			return Mono.zip(encodedData, this.metadataEncoder.encode())
					.map(tuple -> PayloadUtils.createPayload(tuple.getT1(), tuple.getT2()))
					.doOnDiscard(DataBuffer.class, DataBufferUtils::release)
					.doOnDiscard(Payload.class, Payload::release);
		}

		@Override
		public Mono<Void> sendMetadata() {
			return rsocketClient.metadataPush(getPayloadMono());
		}

		@Override
		public Mono<Void> send() {
			return rsocketClient.fireAndForget(getPayloadMono());
		}

		@Override
		public <T> Mono<T> retrieveMono(Class<T> dataType) {
			return retrieveMono(ResolvableType.forClass(dataType));
		}

		@Override
		public <T> Mono<T> retrieveMono(ParameterizedTypeReference<T> dataTypeRef) {
			return retrieveMono(ResolvableType.forType(dataTypeRef));
		}

		@SuppressWarnings("unchecked")
		private <T> Mono<T> retrieveMono(ResolvableType elementType) {
			Mono<Payload> payloadMono = rsocketClient.requestResponse(getPayloadMono());

			if (isVoid(elementType)) {
				return (Mono<T>) payloadMono.then();
			}

			Decoder<?> decoder = strategies.decoder(elementType, dataMimeType);
			return (Mono<T>) payloadMono.map(this::retainDataAndReleasePayload)
					.map(dataBuffer -> decoder.decode(dataBuffer, elementType, dataMimeType, EMPTY_HINTS));
		}

		@Override
		public <T> Flux<T> retrieveFlux(Class<T> dataType) {
			return retrieveFlux(ResolvableType.forClass(dataType));
		}

		@Override
		public <T> Flux<T> retrieveFlux(ParameterizedTypeReference<T> dataTypeRef) {
			return retrieveFlux(ResolvableType.forType(dataTypeRef));
		}

		@SuppressWarnings("unchecked")
		private <T> Flux<T> retrieveFlux(ResolvableType elementType) {

			Flux<Payload> payloadFlux = (this.payloadFlux != null ?
					rsocketClient.requestChannel(this.payloadFlux) :
					rsocketClient.requestStream(getPayloadMono()));

			if (isVoid(elementType)) {
				return payloadFlux.thenMany(Flux.empty());
			}

			Decoder<?> decoder = strategies.decoder(elementType, dataMimeType);
			return payloadFlux.map(this::retainDataAndReleasePayload).map(dataBuffer ->
					(T) decoder.decode(dataBuffer, elementType, dataMimeType, EMPTY_HINTS));
		}

		private Mono<Payload> getPayloadMono() {
			Assert.state(this.payloadFlux == null, "No RSocket interaction with Flux request and Mono response.");
			return this.payloadMono != null ? this.payloadMono : firstPayload(emptyBufferMono);
		}

		private DataBuffer retainDataAndReleasePayload(Payload payload) {
			return PayloadUtils.retainDataAndReleasePayload(payload, bufferFactory());
		}
	}

}
