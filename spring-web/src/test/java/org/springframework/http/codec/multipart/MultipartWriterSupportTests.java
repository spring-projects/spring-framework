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

package org.springframework.http.codec.multipart;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultipartWriterSupportTests extends AbstractLeakCheckingTests {

	private static final byte[] BOUNDARY = "boundary".getBytes(StandardCharsets.US_ASCII);

	private static final String HEADERS =
			"Content-Disposition: form-data; name=\"part\"\r\nContent-Type: text/plain\r\n\r\n";

	private static final ResolvableType STRING_TYPE = ResolvableType.forClass(String.class);


	private final MultipartWriterSupport writer = new MultipartWriterSupport(List.of(MediaType.MULTIPART_FORM_DATA));

	private final ReactiveHttpOutputMessage response = mock();

	private final ArgumentCaptor<Publisher<? extends Publisher<? extends DataBuffer>>> bodyCaptor =
			ArgumentCaptor.captor();

	private final StringDecoder decoder = StringDecoder.textPlainOnly();


	@BeforeEach
	void setUp() {
		when(this.response.bufferFactory()).thenReturn(this.bufferFactory);
		when(this.response.writeAndFlushWith(this.bodyCaptor.capture())).thenReturn(Mono.empty());
	}


	@Test
	void generateBoundary() {
		Mono<DataBuffer> result = this.writer.generateBoundary(BOUNDARY, this.bufferFactory);

		StepVerifier.create(this.decoder.decodeToMono(result, STRING_TYPE, MediaType.TEXT_PLAIN, Map.of()))
				.expectNext("--boundary")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void generateLastLineSuffix() {
		Mono<DataBuffer> result = this.writer.generateLastLineSuffix(this.bufferFactory);

		StepVerifier.create(this.decoder.decodeToMono(result, STRING_TYPE, MediaType.TEXT_PLAIN, Map.of()))
				.expectNext("--\r\n")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void flushCompletedPartBeforeNextBody() {
		Sinks.Many<String> nextBody = Sinks.many().unicast().onBackpressureBuffer();
		String firstPart = "\r\n" + HEADERS + "first\r\n--boundary";
		String secondPart = "\r\n" + HEADERS + "second\r\n--boundary";
		Flux<Publisher<DataBuffer>> parts = Flux.just(
				Flux.just(HEADERS + "first").map(this::buffer),
				Flux.concat(Flux.just(HEADERS), nextBody.asFlux()).map(this::buffer));

		this.writer.writeMultipart(parts, BOUNDARY, this.response, Map.of());

		Flux<String> flushGroups = Flux.from(this.bodyCaptor.getValue())
				.concatMap(group -> this.decoder.decodeToMono(
						Flux.from(group), STRING_TYPE, MediaType.TEXT_PLAIN, Map.of()));
		StepVerifier.create(flushGroups)
				.expectNext("--boundary", firstPart)
				.then(() -> assertThat(nextBody.tryEmitNext("second")).isEqualTo(Sinks.EmitResult.OK))
				.then(() -> assertThat(nextBody.tryEmitComplete()).isEqualTo(Sinks.EmitResult.OK))
				.expectNext(secondPart, "--\r\n")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void flushCompletedPartBeforeNextPartAndStreamCompletion() {
		Sinks.Many<Publisher<DataBuffer>> parts = Sinks.many().unicast().onBackpressureBuffer();
		String firstPart = "\r\n" + HEADERS + "first\r\n--boundary";
		String secondPart = "\r\n" + HEADERS + "second\r\n--boundary";

		this.writer.writeMultipart(parts.asFlux(), BOUNDARY, this.response, Map.of());

		Flux<String> flushGroups = Flux.from(this.bodyCaptor.getValue())
				.concatMap(group -> this.decoder.decodeToMono(
						Flux.from(group), STRING_TYPE, MediaType.TEXT_PLAIN, Map.of()));
		StepVerifier.create(flushGroups)
				.then(() -> assertThat(parts.tryEmitNext(Flux.just(HEADERS + "first").map(this::buffer)))
						.isEqualTo(Sinks.EmitResult.OK))
				.expectNext("--boundary", firstPart)
				.then(() -> assertThat(parts.tryEmitNext(Flux.just(HEADERS + "second").map(this::buffer)))
						.isEqualTo(Sinks.EmitResult.OK))
				.expectNext(secondPart)
				.then(() -> assertThat(parts.tryEmitComplete()).isEqualTo(Sinks.EmitResult.OK))
				.expectNext("--\r\n")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void emptyMultipart() {
		this.writer.writeMultipart(Flux.empty(), BOUNDARY, this.response, Map.of());

		Flux<String> flushGroups = Flux.from(this.bodyCaptor.getValue())
				.concatMap(group -> this.decoder.decodeToMono(
						Flux.from(group), STRING_TYPE, MediaType.TEXT_PLAIN, Map.of()));
		StepVerifier.create(flushGroups)
				.expectNext("--boundary--\r\n")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void emptyPart() {
		String part = "\r\n" + HEADERS + "\r\n--boundary";
		Flux<Publisher<DataBuffer>> parts = Flux.just(Flux.just(HEADERS).map(this::buffer));

		this.writer.writeMultipart(parts, BOUNDARY, this.response, Map.of());

		Flux<String> flushGroups = Flux.from(this.bodyCaptor.getValue())
				.concatMap(group -> this.decoder.decodeToMono(
						Flux.from(group), STRING_TYPE, MediaType.TEXT_PLAIN, Map.of()));
		StepVerifier.create(flushGroups)
				.expectNext("--boundary", part, "--\r\n")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void monoPart() {
		DefaultDataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;
		when(this.response.bufferFactory()).thenReturn(bufferFactory);
		byte[] content = (HEADERS + "value").getBytes(StandardCharsets.UTF_8);
		Mono<Mono<DefaultDataBuffer>> parts = Mono.just(Mono.fromSupplier(() -> bufferFactory.wrap(content)));

		this.writer.writeMultipart(parts, BOUNDARY, this.response, Map.of());

		Flux<String> flushGroups = Flux.from(this.bodyCaptor.getValue())
				.concatMap(group -> this.decoder.decodeToMono(
						Flux.from(group), STRING_TYPE, MediaType.TEXT_PLAIN, Map.of()));
		StepVerifier.create(flushGroups)
				.expectNext("--boundary", "\r\n" + HEADERS + "value\r\n--boundary", "--\r\n")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void multipleBuffersStayInOnePart() {
		Flux<Flux<DataBuffer>> parts = Flux.just(
				Flux.just(HEADERS, "first", "second").map(this::buffer));

		this.writer.writeMultipart(parts, BOUNDARY, this.response, Map.of());

		Flux<String> flushGroups = Flux.from(this.bodyCaptor.getValue())
				.concatMap(group -> this.decoder.decodeToMono(
						Flux.from(group), STRING_TYPE, MediaType.TEXT_PLAIN, Map.of()));
		StepVerifier.create(flushGroups)
				.expectNext("--boundary", "\r\n" + HEADERS + "firstsecond\r\n--boundary", "--\r\n")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void bodyErrorDoesNotCompletePart() {
		IllegalStateException error = new IllegalStateException("body failed");
		Flux<Publisher<DataBuffer>> parts = Flux.just(
				Flux.concat(Flux.just(HEADERS, "partial"), Flux.<String>error(error)).map(this::buffer));

		this.writer.writeMultipart(parts, BOUNDARY, this.response, Map.of());

		Flux<String> flushGroups = Flux.from(this.bodyCaptor.getValue())
				.concatMap(group -> this.decoder.decodeToMono(
						Flux.from(group), STRING_TYPE, MediaType.TEXT_PLAIN, Map.of()));
		StepVerifier.create(flushGroups)
				.expectNext("--boundary")
				.expectErrorSatisfies(actual -> assertThat(actual).isSameAs(error))
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void sourceErrorDoesNotWriteBoundary() {
		IllegalStateException error = new IllegalStateException("source failed");
		MockServerHttpResponse response = new MockServerHttpResponse(this.bufferFactory);
		response.getHeaders().setContentType(
				this.writer.getMultipartMediaType(MediaType.MULTIPART_FORM_DATA, BOUNDARY));

		Mono<Void> result = this.writer.writeMultipart(Flux.error(error), BOUNDARY, response, Map.of());

		StepVerifier.create(result)
				.expectErrorSatisfies(actual -> assertThat(actual).isSameAs(error))
				.verify(Duration.ofSeconds(5));
		assertThat(response.isCommitted()).isFalse();
	}

	@Test
	void cancellationDoesNotCloseMultipart() {
		AtomicBoolean cancelled = new AtomicBoolean();
		String firstPart = HEADERS + "first";
		Flux<Publisher<DataBuffer>> parts = Flux.just(
				Flux.just(firstPart).map(this::buffer),
				Flux.concat(Flux.just(HEADERS), Flux.<String>never())
						.map(this::buffer).doOnCancel(() -> cancelled.set(true)));

		this.writer.writeMultipart(parts, BOUNDARY, this.response, Map.of());

		Flux<String> body = Flux.from(this.bodyCaptor.getValue())
				.concatMap(Flux::from)
				.map(buffer -> this.decoder.decode(buffer, STRING_TYPE, MediaType.TEXT_PLAIN, Map.of()));
		StepVerifier.create(body)
				.expectNext("--boundary", "\r\n", firstPart, "\r\n", "--boundary", "\r\n", HEADERS)
				.thenCancel()
				.verify(Duration.ofSeconds(5));
		assertThat(cancelled).isTrue();
	}

	private DataBuffer buffer(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		return this.bufferFactory.allocateBuffer(bytes.length).write(bytes);
	}

}
