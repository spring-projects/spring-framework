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
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.RSocketRequester.RequestSpec;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN;

/**
 * Unit tests for {@link DefaultRSocketRequester}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultRSocketRequesterTests {

	private static final Duration MILLIS_10 = Duration.ofMillis(10);


	private TestRSocket rsocket;

	private RSocketRequester requester;

	private final RSocketStrategies strategies = RSocketStrategies.create();

	private final DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	@BeforeEach
	public void setUp() {
		this.rsocket = new TestRSocket();
		this.requester = RSocketRequester.wrap(this.rsocket, TEXT_PLAIN, TEXT_PLAIN, this.strategies);
	}


	@Test
	public void sendMono() {

		// data(Object)
		testSendMono(spec -> spec.data("bodyA"), "bodyA");
		testSendMono(spec -> spec.data(Mono.delay(MILLIS_10).map(l -> "bodyA")), "bodyA");
		testSendMono(spec -> spec.data(Mono.delay(MILLIS_10).then()), "");
		testSendMono(spec -> spec.data(Single.timer(10, MILLISECONDS).map(l -> "bodyA")), "bodyA");
		testSendMono(spec -> spec.data(Completable.complete()), "");

		// data(Publisher<T>, Class<T>)
		testSendMono(spec -> spec.data(Mono.delay(MILLIS_10).map(l -> "bodyA"), String.class), "bodyA");
		testSendMono(spec -> spec.data(Mono.delay(MILLIS_10).map(l -> "bodyA"), Object.class), "bodyA");
		testSendMono(spec -> spec.data(Mono.delay(MILLIS_10).then(), Void.class), "");
	}

	private void testSendMono(Function<RequestSpec, RequestSpec> mapper, String expectedValue) {
		mapper.apply(this.requester.route("toA")).send().block(Duration.ofSeconds(5));

		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("fireAndForget");
		assertThat(this.rsocket.getSavedPayload().getMetadataUtf8()).isEqualTo("toA");
		assertThat(this.rsocket.getSavedPayload().getDataUtf8()).isEqualTo(expectedValue);
	}

	@Test
	public void sendFlux() {
		String[] values = new String[] {"bodyA", "bodyB", "bodyC"};
		Flux<String> stringFlux = Flux.fromArray(values).delayElements(MILLIS_10);

		// data(Object)
		testSendFlux(spec -> spec.data(stringFlux), values);
		testSendFlux(spec -> spec.data(Flux.empty()), "");
		testSendFlux(spec -> spec.data(Observable.fromArray(values).delay(10, MILLISECONDS)), values);
		testSendFlux(spec -> spec.data(Observable.empty()), "");

		// data(Publisher<T>, Class<T>)
		testSendFlux(spec -> spec.data(stringFlux, String.class), values);
		testSendFlux(spec -> spec.data(stringFlux.cast(Object.class), Object.class), values);
	}

	private void testSendFlux(Function<RequestSpec, RequestSpec> mapper, String... expectedValues) {
		this.rsocket.reset();
		mapper.apply(this.requester.route("toA")).retrieveFlux(String.class).blockLast(Duration.ofSeconds(5));

		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestChannel");
		List<Payload> payloads = this.rsocket.getSavedPayloadFlux().collectList().block(Duration.ofSeconds(5));
		assertThat(payloads).isNotNull();

		if (Arrays.equals(new String[] {""}, expectedValues)) {
			assertThat(payloads.size()).isEqualTo(1);
			assertThat(payloads.get(0).getMetadataUtf8()).isEqualTo("toA");
			assertThat(payloads.get(0).getDataUtf8()).isEqualTo("");
		}
		else {
			assertThat(payloads.stream().map(Payload::getMetadataUtf8).toArray(String[]::new))
					.isEqualTo(new String[] {"toA", "", ""});
			assertThat(payloads.stream().map(Payload::getDataUtf8).toArray(String[]::new))
					.isEqualTo(expectedValues);
		}
	}

	@Test
	public void retrieveMono() {
		String value = "bodyA";
		this.rsocket.setPayloadMonoToReturn(Mono.delay(MILLIS_10).thenReturn(toPayload(value)));
		Mono<String> response = this.requester.route("").data("").retrieveMono(String.class);

		StepVerifier.create(response).expectNext(value).expectComplete().verify(Duration.ofSeconds(5));
		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestResponse");
	}

	@Test
	public void retrieveMonoVoid() {
		AtomicBoolean consumed = new AtomicBoolean(false);
		Mono<Payload> mono = Mono.delay(MILLIS_10).thenReturn(toPayload("bodyA")).doOnSuccess(p -> consumed.set(true));
		this.rsocket.setPayloadMonoToReturn(mono);
		this.requester.route("").data("").retrieveMono(Void.class).block(Duration.ofSeconds(5));

		assertThat(consumed.get()).isTrue();
		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestResponse");
	}

	@Test
	public void retrieveFlux() {
		String[] values = new String[] {"bodyA", "bodyB", "bodyC"};
		this.rsocket.setPayloadFluxToReturn(Flux.fromArray(values).delayElements(MILLIS_10).map(this::toPayload));
		Flux<String> response = this.requester.route("").data("").retrieveFlux(String.class);

		StepVerifier.create(response).expectNext(values).expectComplete().verify(Duration.ofSeconds(5));
		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestStream");
	}

	@Test
	public void retrieveFluxVoid() {
		AtomicBoolean consumed = new AtomicBoolean(false);
		Flux<Payload> flux = Flux.just("bodyA", "bodyB")
				.delayElements(MILLIS_10).map(this::toPayload).doOnComplete(() -> consumed.set(true));
		this.rsocket.setPayloadFluxToReturn(flux);
		this.requester.route("").data("").retrieveFlux(Void.class).blockLast(Duration.ofSeconds(5));

		assertThat(consumed.get()).isTrue();
		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestStream");
	}

	@Test
	public void fluxToMonoIsRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.requester.route("").data(Flux.just("a", "b")).retrieveMono(String.class))
				.withMessage("No RSocket interaction model for Flux request to Mono response.");
	}

	private Payload toPayload(String value) {
		return PayloadUtils.createPayload(bufferFactory.wrap(value.getBytes(StandardCharsets.UTF_8)));
	}


	private static class TestRSocket extends AbstractRSocket {

		private Mono<Payload> payloadMonoToReturn = Mono.empty();
		private Flux<Payload> payloadFluxToReturn = Flux.empty();

		@Nullable private volatile String savedMethodName;
		@Nullable private volatile Payload savedPayload;
		@Nullable private volatile Flux<Payload> savedPayloadFlux;

		void setPayloadMonoToReturn(Mono<Payload> payloadMonoToReturn) {
			this.payloadMonoToReturn = payloadMonoToReturn;
		}

		void setPayloadFluxToReturn(Flux<Payload> payloadFluxToReturn) {
			this.payloadFluxToReturn = payloadFluxToReturn;
		}

		@Nullable
		String getSavedMethodName() {
			return this.savedMethodName;
		}

		@Nullable
		Payload getSavedPayload() {
			return this.savedPayload;
		}

		@Nullable
		Flux<Payload> getSavedPayloadFlux() {
			return this.savedPayloadFlux;
		}

		public void reset() {
			this.savedMethodName = null;
			this.savedPayload = null;
			this.savedPayloadFlux = null;
		}


		@Override
		public Mono<Void> fireAndForget(Payload payload) {
			this.savedMethodName = "fireAndForget";
			this.savedPayload = payload;
			return Mono.empty();
		}

		@Override
		public Mono<Payload> requestResponse(Payload payload) {
			this.savedMethodName = "requestResponse";
			this.savedPayload = payload;
			return this.payloadMonoToReturn;
		}

		@Override
		public Flux<Payload> requestStream(Payload payload) {
			this.savedMethodName = "requestStream";
			this.savedPayload = payload;
			return this.payloadFluxToReturn;
		}

		@Override
		public Flux<Payload> requestChannel(Publisher<Payload> publisher) {
			this.savedMethodName = "requestChannel";
			this.savedPayloadFlux = Flux.from(publisher);
			return this.payloadFluxToReturn;
		}
	}

}
