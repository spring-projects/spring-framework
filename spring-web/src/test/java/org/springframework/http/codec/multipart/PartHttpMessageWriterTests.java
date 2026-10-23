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
import java.util.Collections;
import java.util.Map;

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
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.codec.multipart.MultipartHttpMessageWriterTests.parse;

/**
 * Tests for {@link PartHttpMessageWriter}.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
class PartHttpMessageWriterTests extends AbstractLeakCheckingTests {

	private final PartHttpMessageWriter writer = new PartHttpMessageWriter();

	private final MockServerHttpResponse response = new MockServerHttpResponse(this.bufferFactory);


	@Test
	void canWrite() {
		assertThat(this.writer.canWrite(ResolvableType.forClass(Part.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
		assertThat(this.writer.canWrite(ResolvableType.forClass(Part.class), MediaType.MULTIPART_MIXED)).isTrue();
		assertThat(this.writer.canWrite(ResolvableType.forClass(Part.class), MediaType.MULTIPART_RELATED)).isTrue();
		assertThat(this.writer.canWrite(ResolvableType.forClass(MultiValueMap.class), MediaType.MULTIPART_FORM_DATA)).isFalse();
	}

	@Test
	void write() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		Part textPart = mock();
		given(textPart.name()).willReturn("text part");
		given(textPart.headers()).willReturn(headers);
		given(textPart.content()).willReturn(Flux.just(
				this.bufferFactory.wrap("text1".getBytes(StandardCharsets.UTF_8)),
				this.bufferFactory.wrap("text2".getBytes(StandardCharsets.UTF_8))));

		FilePart filePart = mock();
		given(filePart.name()).willReturn("file part");
		given(filePart.headers()).willReturn(new HttpHeaders());
		given(filePart.filename()).willReturn("file.txt");
		given(filePart.content()).willReturn(Flux.just(
				this.bufferFactory.wrap("Aa".getBytes(StandardCharsets.UTF_8)),
				this.bufferFactory.wrap("Bb".getBytes(StandardCharsets.UTF_8)),
				this.bufferFactory.wrap("Cc".getBytes(StandardCharsets.UTF_8))
		));

		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(Flux.just(textPart, filePart), null, MediaType.MULTIPART_FORM_DATA, this.response, hints)
				.block(Duration.ofSeconds(5));

		MultiValueMap<String, Part> requestParts = parse(this.response, hints);
		assertThat(requestParts).hasSize(2);

		Part part = requestParts.getFirst("text part");
		assertThat(part.name()).isEqualTo("text part");
		assertThat(part.headers().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		String value = decodeToString(part);
		assertThat(value).isEqualTo("text1text2");

		part = requestParts.getFirst("file part");
		assertThat(part.name()).isEqualTo("file part");
		assertThat(((FilePart) part).filename()).isEqualTo("file.txt");
		assertThat(decodeToString(part)).isEqualTo("AaBbCc");
	}

	@Test
	void flushCompletedPartBeforeNextPartAndStreamCompletion() {
		ReactiveHttpOutputMessage outputMessage = mock();
		HttpHeaders outputHeaders = new HttpHeaders();
		ArgumentCaptor<Publisher<? extends Publisher<? extends DataBuffer>>> bodyCaptor = ArgumentCaptor.captor();
		when(outputMessage.getHeaders()).thenReturn(outputHeaders);
		when(outputMessage.bufferFactory()).thenReturn(this.bufferFactory);
		when(outputMessage.writeAndFlushWith(bodyCaptor.capture())).thenReturn(Mono.empty());

		Sinks.Many<Part> parts = Sinks.many().unicast().onBackpressureBuffer();
		Sinks.Many<String> firstBody = Sinks.many().unicast().onBackpressureBuffer();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDispositionFormData("part", null);
		headers.setContentType(MediaType.TEXT_PLAIN);
		Part firstPart = DefaultParts.part(headers, firstBody.asFlux()
				.map(value -> this.bufferFactory.wrap(value.getBytes(StandardCharsets.UTF_8))));
		Part secondPart = DefaultParts.formFieldPart(headers, "second");

		this.writer.write(parts.asFlux(), ResolvableType.forClass(Part.class),
				MediaType.MULTIPART_FORM_DATA, outputMessage, Map.of()).block(Duration.ofSeconds(5));

		Flux<DataBuffer> flushedBody = Flux.from(bodyCaptor.getValue()).concatMap(DataBufferUtils::join);
		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.contentType(outputHeaders.getContentType()).body(flushedBody);
		Flux<String> values = new DefaultPartHttpMessageReader()
				.read(ResolvableType.forClass(Part.class), request, Map.of())
				.cast(FormFieldPart.class).map(FormFieldPart::value);
		StepVerifier.create(values)
				.then(() -> assertThat(parts.tryEmitNext(firstPart)).isEqualTo(Sinks.EmitResult.OK))
				.then(() -> assertThat(firstBody.tryEmitNext("first")).isEqualTo(Sinks.EmitResult.OK))
				.then(() -> assertThat(firstBody.tryEmitNext("part")).isEqualTo(Sinks.EmitResult.OK))
				.then(() -> assertThat(firstBody.tryEmitComplete()).isEqualTo(Sinks.EmitResult.OK))
				.expectNext("firstpart")
				.then(() -> assertThat(parts.tryEmitNext(secondPart)).isEqualTo(Sinks.EmitResult.OK))
				.expectNext("second")
				.then(() -> assertThat(parts.tryEmitComplete()).isEqualTo(Sinks.EmitResult.OK))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void emptyPart() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDispositionFormData("part", null);
		headers.setContentType(MediaType.TEXT_PLAIN);
		Part part = DefaultParts.part(headers, Flux.empty());

		this.writer.write(Mono.just(part), ResolvableType.forClass(Part.class),
				MediaType.MULTIPART_FORM_DATA, this.response, Map.of()).block(Duration.ofSeconds(5));

		MultiValueMap<String, Part> parts = parse(this.response, Map.of());
		assertThat(parts).hasSize(1);
		Part parsedPart = parts.getFirst("part");
		assertThat(parsedPart).isInstanceOf(FormFieldPart.class);
		assertThat(((FormFieldPart) parsedPart).value()).isEmpty();
	}

	@SuppressWarnings("ConstantConditions")
	private String decodeToString(Part part) {
		return StringDecoder.textPlainOnly().decodeToMono(part.content(),
				ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN,
				Collections.emptyMap()).block(Duration.ZERO);
	}

}
