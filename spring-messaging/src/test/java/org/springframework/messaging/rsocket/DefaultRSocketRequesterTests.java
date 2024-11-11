/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.rsocket.Payload;
import io.rsocket.metadata.WellKnownMimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.messaging.rsocket.RSocketRequester.RequestSpec;
import org.springframework.messaging.rsocket.RSocketRequester.RetrieveSpec;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.util.MimeTypeUtils.TEXT_PLAIN;

/**
 * Tests for {@link DefaultRSocketRequester}.
 *
 * @author Rossen Stoyanchev
 */
class DefaultRSocketRequesterTests {

	private static final Duration MILLIS_10 = Duration.ofMillis(10);


	private TestRSocket rsocket;

	private RSocketRequester requester;

	private final RSocketStrategies strategies = RSocketStrategies.create();


	@BeforeEach
	void setUp() {
		this.rsocket = new TestRSocket();
		this.requester = RSocketRequester.wrap(this.rsocket, TEXT_PLAIN, TEXT_PLAIN, this.strategies);
	}


	@Test
	void sendMono() {

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

	private void testSendMono(Function<RequestSpec, RetrieveSpec> mapper, String expectedValue) {
		mapper.apply(this.requester.route("toA")).send().block(Duration.ofSeconds(5));

		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("fireAndForget");
		assertThat(this.rsocket.getSavedPayload().getMetadataUtf8()).isEqualTo("toA");
		assertThat(this.rsocket.getSavedPayload().getDataUtf8()).isEqualTo(expectedValue);
	}

	@Test
	void sendFlux() {
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

	private void testSendFlux(Function<RequestSpec, RetrieveSpec> mapper, String... expectedValues) {
		this.rsocket.reset();
		mapper.apply(this.requester.route("toA")).retrieveFlux(String.class).blockLast(Duration.ofSeconds(5));

		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestChannel");
		List<Payload> payloads = this.rsocket.getSavedPayloadFlux().collectList().block(Duration.ofSeconds(5));
		assertThat(payloads).isNotNull();

		if (Arrays.equals(new String[] {""}, expectedValues)) {
			assertThat(payloads).hasSize(1);
			assertThat(payloads.get(0).getMetadataUtf8()).isEqualTo("toA");
			assertThat(payloads.get(0).getDataUtf8()).isEmpty();
		}
		else {
			assertThat(payloads.stream().map(Payload::getMetadataUtf8).toArray(String[]::new))
					.isEqualTo(new String[] {"toA", "", ""});
			assertThat(payloads.stream().map(Payload::getDataUtf8).toArray(String[]::new))
					.isEqualTo(expectedValues);
		}
	}

	@Test
	void sendWithoutData() {
		this.requester.route("toA").send().block(Duration.ofSeconds(5));

		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("fireAndForget");
		assertThat(this.rsocket.getSavedPayload().getMetadataUtf8()).isEqualTo("toA");
		assertThat(this.rsocket.getSavedPayload().getDataUtf8()).isEmpty();
	}

	@Test
	void testSendWithAsyncMetadata() {

		MimeType compositeMimeType =
				MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());

		Mono<String> asyncMeta1 = Mono.delay(Duration.ofMillis(1)).map(aLong -> "Async Metadata 1");
		Mono<String> asyncMeta2 = Mono.delay(Duration.ofMillis(1)).map(aLong -> "Async Metadata 2");

		TestRSocket rsocket = new TestRSocket();
		RSocketRequester.wrap(rsocket, TEXT_PLAIN, compositeMimeType, this.strategies)
				.route("toA")
				.metadata(asyncMeta1, new MimeType("text", "x.test.metadata1"))
				.metadata(asyncMeta2, new MimeType("text", "x.test.metadata2"))
				.data("data")
				.send()
				.block(Duration.ofSeconds(5));

		Payload payload = rsocket.getSavedPayload();

		DefaultMetadataExtractor extractor = new DefaultMetadataExtractor(this.strategies.decoders());
		extractor.metadataToExtract(new MimeType("text", "x.test.metadata1"), String.class, "asyncMeta1");
		extractor.metadataToExtract(new MimeType("text", "x.test.metadata2"), String.class, "asyncMeta2");
		Map<String, Object> metadataValues = extractor.extract(payload, compositeMimeType);

		assertThat(metadataValues.get("asyncMeta1")).isEqualTo("Async Metadata 1");
		assertThat(metadataValues.get("asyncMeta2")).isEqualTo("Async Metadata 2");
		assertThat(payload.getDataUtf8()).isEqualTo("data");
	}

	@Test
	void retrieveMono() {
		String value = "bodyA";
		this.rsocket.setPayloadMonoToReturn(Mono.delay(MILLIS_10).thenReturn(toPayload(value)));
		Mono<String> response = this.requester.route("").data("").retrieveMono(String.class);

		StepVerifier.create(response).expectNext(value).expectComplete().verify(Duration.ofSeconds(5));
		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestResponse");
	}

	@Test
	void retrieveMonoVoid() {
		AtomicBoolean consumed = new AtomicBoolean();
		Mono<Payload> mono = Mono.delay(MILLIS_10).thenReturn(toPayload("bodyA")).doOnSuccess(p -> consumed.set(true));
		this.rsocket.setPayloadMonoToReturn(mono);
		this.requester.route("").data("").retrieveMono(Void.class).block(Duration.ofSeconds(5));

		assertThat(consumed.get()).isTrue();
		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestResponse");
	}

	@Test
	void retrieveMonoWithoutData() {
		this.requester.route("toA").retrieveMono(String.class).block(Duration.ofSeconds(5));

		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestResponse");
		assertThat(this.rsocket.getSavedPayload().getMetadataUtf8()).isEqualTo("toA");
		assertThat(this.rsocket.getSavedPayload().getDataUtf8()).isEmpty();
	}

	@Test
	void retrieveFlux() {
		String[] values = new String[] {"bodyA", "bodyB", "bodyC"};
		this.rsocket.setPayloadFluxToReturn(Flux.fromArray(values).delayElements(MILLIS_10).map(this::toPayload));
		Flux<String> response = this.requester.route("").data("").retrieveFlux(String.class);

		StepVerifier.create(response).expectNext(values).expectComplete().verify(Duration.ofSeconds(5));
		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestStream");
	}

	@Test
	void retrieveFluxVoid() {
		AtomicBoolean consumed = new AtomicBoolean();
		Flux<Payload> flux = Flux.just("bodyA", "bodyB")
				.delayElements(MILLIS_10).map(this::toPayload).doOnComplete(() -> consumed.set(true));
		this.rsocket.setPayloadFluxToReturn(flux);
		this.requester.route("").data("").retrieveFlux(Void.class).blockLast(Duration.ofSeconds(5));

		assertThat(consumed.get()).isTrue();
		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestStream");
	}

	@Test
	void retrieveFluxWithoutData() {
		this.requester.route("toA").retrieveFlux(String.class).blockLast(Duration.ofSeconds(5));

		assertThat(this.rsocket.getSavedMethodName()).isEqualTo("requestStream");
		assertThat(this.rsocket.getSavedPayload().getMetadataUtf8()).isEqualTo("toA");
		assertThat(this.rsocket.getSavedPayload().getDataUtf8()).isEmpty();
	}

	@Test
	void fluxToMonoIsRejected() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.requester.route("").data(Flux.just("a", "b")).retrieveMono(String.class))
				.withMessage("No RSocket interaction with Flux request and Mono response.");
	}

	private Payload toPayload(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		return PayloadUtils.createPayload(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
	}

}
