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
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.RSocketRequester.RequestSpec;
import org.springframework.messaging.rsocket.RSocketRequester.ResponseSpec;
import org.springframework.util.MimeTypeUtils;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link DefaultRSocketRequester}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultRSocketRequesterTests {

	private static final Duration MILLIS_10 = Duration.ofMillis(10);


	private TestRSocket rsocket;

	private RSocketRequester requester;

	private final DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	@Before
	public void setUp() {
		RSocketStrategies strategies = RSocketStrategies.builder()
				.decoder(StringDecoder.allMimeTypes())
				.encoder(CharSequenceEncoder.allMimeTypes())
				.build();
		this.rsocket = new TestRSocket();
		this.requester = RSocketRequester.create(rsocket, MimeTypeUtils.TEXT_PLAIN, strategies);
	}


	@Test
	public void singlePayload() {

		// data(Object)
		testSinglePayload(spec -> spec.data("bodyA"), "bodyA");
		testSinglePayload(spec -> spec.data(Mono.delay(MILLIS_10).map(l -> "bodyA")), "bodyA");
		testSinglePayload(spec -> spec.data(Mono.delay(MILLIS_10).then()), "");
		testSinglePayload(spec -> spec.data(Single.timer(10, MILLISECONDS).map(l -> "bodyA")), "bodyA");
		testSinglePayload(spec -> spec.data(Completable.complete()), "");

		// data(Publisher<T>, Class<T>)
		testSinglePayload(spec -> spec.data(Mono.delay(MILLIS_10).map(l -> "bodyA"), String.class), "bodyA");
		testSinglePayload(spec -> spec.data(Mono.delay(MILLIS_10).map(l -> "bodyA"), Object.class), "bodyA");
		testSinglePayload(spec -> spec.data(Mono.delay(MILLIS_10).then(), Void.class), "");
	}

	private void testSinglePayload(Function<RequestSpec, ResponseSpec> mapper, String expectedValue) {
		mapper.apply(this.requester.route("toA")).send().block(Duration.ofSeconds(5));

		assertEquals("fireAndForget", this.rsocket.getSavedMethodName());
		assertEquals("toA", this.rsocket.getSavedPayload().getMetadataUtf8());
		assertEquals(expectedValue, this.rsocket.getSavedPayload().getDataUtf8());
	}

	@Test
	public void multiPayload() {
		String[] values = new String[] {"bodyA", "bodyB", "bodyC"};
		Flux<String> stringFlux = Flux.fromArray(values).delayElements(MILLIS_10);

		// data(Object)
		testMultiPayload(spec -> spec.data(stringFlux), values);
		testMultiPayload(spec -> spec.data(Flux.empty()), "");
		testMultiPayload(spec -> spec.data(Observable.fromArray(values).delay(10, MILLISECONDS)), values);
		testMultiPayload(spec -> spec.data(Observable.empty()), "");

		// data(Publisher<T>, Class<T>)
		testMultiPayload(spec -> spec.data(stringFlux, String.class), values);
		testMultiPayload(spec -> spec.data(stringFlux.cast(Object.class), Object.class), values);
	}

	private void testMultiPayload(Function<RequestSpec, ResponseSpec> mapper, String... expectedValues) {
		this.rsocket.reset();
		mapper.apply(this.requester.route("toA")).retrieveFlux(String.class).blockLast(Duration.ofSeconds(5));

		assertEquals("requestChannel", this.rsocket.getSavedMethodName());
		List<Payload> payloads = this.rsocket.getSavedPayloadFlux().collectList().block(Duration.ofSeconds(5));
		assertNotNull(payloads);

		if (Arrays.equals(new String[] {""}, expectedValues)) {
			assertEquals(1, payloads.size());
			assertEquals("toA", payloads.get(0).getMetadataUtf8());
			assertEquals("", payloads.get(0).getDataUtf8());
		}
		else {
			assertArrayEquals(new String[] {"toA", "", ""},
					payloads.stream().map(Payload::getMetadataUtf8).toArray(String[]::new));
			assertArrayEquals(expectedValues,
					payloads.stream().map(Payload::getDataUtf8).toArray(String[]::new));
		}
	}

	@Test
	public void send() {
		String value = "bodyA";
		this.requester.route("toA").data(value).send().block(Duration.ofSeconds(5));

		assertEquals("fireAndForget", this.rsocket.getSavedMethodName());
		assertEquals("toA", this.rsocket.getSavedPayload().getMetadataUtf8());
		assertEquals("bodyA", this.rsocket.getSavedPayload().getDataUtf8());
	}

	@Test
	public void retrieveMono() {
		String value = "bodyA";
		this.rsocket.setPayloadMonoToReturn(Mono.delay(MILLIS_10).thenReturn(toPayload(value)));
		Mono<String> response = this.requester.route("").data("").retrieveMono(String.class);

		StepVerifier.create(response).expectNext(value).expectComplete().verify(Duration.ofSeconds(5));
		assertEquals("requestResponse", this.rsocket.getSavedMethodName());
	}

	@Test
	public void retrieveMonoVoid() {
		AtomicBoolean consumed = new AtomicBoolean(false);
		Mono<Payload> mono = Mono.delay(MILLIS_10).thenReturn(toPayload("bodyA")).doOnSuccess(p -> consumed.set(true));
		this.rsocket.setPayloadMonoToReturn(mono);
		this.requester.route("").data("").retrieveMono(Void.class).block(Duration.ofSeconds(5));

		assertTrue(consumed.get());
		assertEquals("requestResponse", this.rsocket.getSavedMethodName());
	}

	@Test
	public void retrieveFlux() {
		String[] values = new String[] {"bodyA", "bodyB", "bodyC"};
		this.rsocket.setPayloadFluxToReturn(Flux.fromArray(values).delayElements(MILLIS_10).map(this::toPayload));
		Flux<String> response = this.requester.route("").data("").retrieveFlux(String.class);

		StepVerifier.create(response).expectNext(values).expectComplete().verify(Duration.ofSeconds(5));
		assertEquals("requestStream", this.rsocket.getSavedMethodName());
	}

	@Test
	public void retrieveFluxVoid() {
		AtomicBoolean consumed = new AtomicBoolean(false);
		Flux<Payload> flux = Flux.just("bodyA", "bodyB")
				.delayElements(MILLIS_10).map(this::toPayload).doOnComplete(() -> consumed.set(true));
		this.rsocket.setPayloadFluxToReturn(flux);
		this.requester.route("").data("").retrieveFlux(Void.class).blockLast(Duration.ofSeconds(5));

		assertTrue(consumed.get());
		assertEquals("requestStream", this.rsocket.getSavedMethodName());
	}

	@Test
	public void rejectFluxToMono() {
		try {
			this.requester.route("").data(Flux.just("a", "b")).retrieveMono(String.class);
			fail();
		}
		catch (IllegalArgumentException ex) {
			assertEquals("No RSocket interaction model for Flux request to Mono response.", ex.getMessage());
		}
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
