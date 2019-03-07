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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Default, package-private {@link RSocketRequester} implementation.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
final class DefaultRSocketRequester implements RSocketRequester {

	private static final Map<String, Object> EMPTY_HINTS = Collections.emptyMap();


	private final RSocket rsocket;

	@Nullable
	private final MimeType dataMimeType;

	private final RSocketStrategies strategies;

	private DataBuffer emptyDataBuffer;


	DefaultRSocketRequester(RSocket rsocket, @Nullable MimeType dataMimeType, RSocketStrategies strategies) {
		Assert.notNull(rsocket, "RSocket is required");
		Assert.notNull(strategies, "RSocketStrategies is required");
		this.rsocket = rsocket;
		this.dataMimeType = dataMimeType;
		this.strategies = strategies;
		this.emptyDataBuffer = this.strategies.dataBufferFactory().wrap(new byte[0]);
	}


	@Override
	public RSocket rsocket() {
		return this.rsocket;
	}

	@Override
	public RequestSpec route(String route) {
		return new DefaultRequestSpec(route);
	}


	private static boolean isVoid(ResolvableType elementType) {
		return Void.class.equals(elementType.resolve()) || void.class.equals(elementType.resolve());
	}


	private class DefaultRequestSpec implements RequestSpec {

		private final String route;


		DefaultRequestSpec(String route) {
			this.route = route;
		}


		@Override
		public ResponseSpec data(Object data) {
			Assert.notNull(data, "'data' must not be null");
			return toResponseSpec(data, ResolvableType.NONE);
		}

		@Override
		public <T, P extends Publisher<T>> ResponseSpec data(P publisher, Class<T> dataType) {
			Assert.notNull(publisher, "'publisher' must not be null");
			Assert.notNull(dataType, "'dataType' must not be null");
			return toResponseSpec(publisher, ResolvableType.forClass(dataType));
		}

		@Override
		public <T, P extends Publisher<T>> ResponseSpec data(P publisher, ParameterizedTypeReference<T> dataTypeRef) {
			Assert.notNull(publisher, "'publisher' must not be null");
			Assert.notNull(dataTypeRef, "'dataTypeRef' must not be null");
			return toResponseSpec(publisher, ResolvableType.forType(dataTypeRef));
		}

		private ResponseSpec toResponseSpec(Object input, ResolvableType dataType) {
			ReactiveAdapter adapter = strategies.reactiveAdapterRegistry().getAdapter(input.getClass());
			Publisher<?> publisher;
			if (input instanceof Publisher) {
				publisher = (Publisher<?>) input;
			}
			else if (adapter != null) {
				publisher = adapter.toPublisher(input);
			}
			else {
				Mono<Payload> payloadMono = encodeValue(input, ResolvableType.forInstance(input), null)
						.map(this::firstPayload)
						.switchIfEmpty(emptyPayload());
				return new DefaultResponseSpec(payloadMono);
			}

			if (isVoid(dataType) || (adapter != null && adapter.isNoValue())) {
				Mono<Payload> payloadMono = Mono.when(publisher).then(emptyPayload());
				return new DefaultResponseSpec(payloadMono);
			}

			Encoder<?> encoder = dataType != ResolvableType.NONE && !Object.class.equals(dataType.resolve()) ?
					strategies.encoder(dataType, dataMimeType) : null;

			if (adapter != null && !adapter.isMultiValue()) {
				Mono<Payload> payloadMono = Mono.from(publisher)
						.flatMap(value -> encodeValue(value, dataType, encoder))
						.map(this::firstPayload)
						.switchIfEmpty(emptyPayload());
				return new DefaultResponseSpec(payloadMono);
			}

			Flux<Payload> payloadFlux = Flux.from(publisher)
					.concatMap(value -> encodeValue(value, dataType, encoder))
					.switchOnFirst((signal, inner) -> {
						DataBuffer data = signal.get();
						if (data != null) {
							return Flux.concat(
									Mono.just(firstPayload(data)),
									inner.skip(1).map(PayloadUtils::createPayload));
						}
						else {
							return inner.map(PayloadUtils::createPayload);
						}
					})
					.switchIfEmpty(emptyPayload());
			return new DefaultResponseSpec(payloadFlux);
		}

		@SuppressWarnings("unchecked")
		private <T> Mono<DataBuffer> encodeValue(T value, ResolvableType valueType, @Nullable Encoder<?> encoder) {
			if (encoder == null) {
				encoder = strategies.encoder(ResolvableType.forInstance(value), dataMimeType);
			}
			return DataBufferUtils.join(((Encoder<T>) encoder).encode(
					Mono.just(value), strategies.dataBufferFactory(), valueType, dataMimeType, EMPTY_HINTS));
		}

		private Payload firstPayload(DataBuffer data) {
			return PayloadUtils.createPayload(getMetadata(), data);
		}

		private Mono<Payload> emptyPayload() {
			return Mono.fromCallable(() -> firstPayload(emptyDataBuffer));
		}

		private DataBuffer getMetadata() {
			return strategies.dataBufferFactory().wrap(this.route.getBytes(StandardCharsets.UTF_8));
		}
	}


	private class DefaultResponseSpec implements ResponseSpec {

		@Nullable
		private final Mono<Payload> payloadMono;

		@Nullable
		private final Flux<Payload> payloadFlux;


		DefaultResponseSpec(Mono<Payload> payloadMono) {
			this.payloadMono = payloadMono;
			this.payloadFlux = null;
		}

		DefaultResponseSpec(Flux<Payload> payloadFlux) {
			this.payloadMono = null;
			this.payloadFlux = payloadFlux;
		}


		@Override
		public Mono<Void> send() {
			Assert.notNull(this.payloadMono, "No RSocket interaction model for one-way send with Flux.");
			return this.payloadMono.flatMap(rsocket::fireAndForget);
		}

		@Override
		public <T> Mono<T> retrieveMono(Class<T> dataType) {
			return retrieveMono(ResolvableType.forClass(dataType));
		}

		@Override
		public <T> Mono<T> retrieveMono(ParameterizedTypeReference<T> dataTypeRef) {
			return retrieveMono(ResolvableType.forType(dataTypeRef));
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
		private <T> Mono<T> retrieveMono(ResolvableType elementType) {
			Assert.notNull(this.payloadMono,
					"No RSocket interaction model for Flux request to Mono response.");

			Mono<Payload> payloadMono = this.payloadMono.flatMap(rsocket::requestResponse);

			if (isVoid(elementType)) {
				return (Mono<T>) payloadMono.then();
			}

			Decoder<?> decoder = strategies.decoder(elementType, dataMimeType);
			return (Mono<T>) decoder.decodeToMono(
					payloadMono.map(this::retainDataAndReleasePayload), elementType, dataMimeType, EMPTY_HINTS);
		}

		@SuppressWarnings("unchecked")
		private <T> Flux<T> retrieveFlux(ResolvableType elementType) {

			Flux<Payload> payloadFlux = this.payloadMono != null ?
					this.payloadMono.flatMapMany(rsocket::requestStream) :
					rsocket.requestChannel(this.payloadFlux);

			if (isVoid(elementType)) {
				return payloadFlux.thenMany(Flux.empty());
			}

			Decoder<?> decoder = strategies.decoder(elementType, dataMimeType);

			return payloadFlux.map(this::retainDataAndReleasePayload).concatMap(dataBuffer ->
					(Mono<T>) decoder.decodeToMono(Mono.just(dataBuffer), elementType, dataMimeType, EMPTY_HINTS));
		}

		private DataBuffer retainDataAndReleasePayload(Payload payload) {
			return PayloadUtils.retainDataAndReleasePayload(payload, strategies.dataBufferFactory());
		}
	}

}
