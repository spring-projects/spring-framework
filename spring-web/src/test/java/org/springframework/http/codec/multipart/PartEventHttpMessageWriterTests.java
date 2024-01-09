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

package org.springframework.http.codec.multipart;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.codec.multipart.MultipartHttpMessageWriterTests.parse;

/**
 * Tests for {@link PartHttpMessageWriter}.
 *
 * @author Arjen Poutsma
 */
class PartEventHttpMessageWriterTests extends AbstractLeakCheckingTests {

	private final PartEventHttpMessageWriter writer = new PartEventHttpMessageWriter();

	private final MockServerHttpResponse response = new MockServerHttpResponse(this.bufferFactory);


	@Test
	void canWrite() {
		assertThat(this.writer.canWrite(ResolvableType.forClass(PartEvent.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
		assertThat(this.writer.canWrite(ResolvableType.forClass(FilePartEvent.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
		assertThat(this.writer.canWrite(ResolvableType.forClass(FormPartEvent.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
	}

	@Test
	void write() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		Mono<FormPartEvent> formPartEvent = FormPartEvent.create("text part", "text");

		Flux<FilePartEvent> filePartEvents =
				FilePartEvent.create("file part", "file.txt", MediaType.APPLICATION_OCTET_STREAM,
						Flux.just(
								this.bufferFactory.wrap("Aa".getBytes(StandardCharsets.UTF_8)),
								this.bufferFactory.wrap("Bb".getBytes(StandardCharsets.UTF_8)),
								this.bufferFactory.wrap("Cc".getBytes(StandardCharsets.UTF_8))
						));

		Flux<PartEvent> partEvents = Flux.concat(
				formPartEvent,
				filePartEvents
		);

		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(partEvents, null, MediaType.MULTIPART_FORM_DATA, this.response, hints)
				.block(Duration.ofSeconds(5));

		MultiValueMap<String, Part> requestParts = parse(this.response, hints);
		assertThat(requestParts).hasSize(2);

		Part part = requestParts.getFirst("text part");
		assertThat(part.name()).isEqualTo("text part");
		assertThat(part.headers().getContentType().isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
		String value = decodeToString(part);
		assertThat(value).isEqualTo("text");

		part = requestParts.getFirst("file part");
		assertThat(part.name()).isEqualTo("file part");
		assertThat(((FilePart) part).filename()).isEqualTo("file.txt");
		assertThat(decodeToString(part)).isEqualTo("AaBbCc");
	}

	@Test
	void writeFormEventStream() {
		Flux<Map.Entry<String, String>> body = Flux.just(
				new AbstractMap.SimpleEntry<>("name 1", "value 1"),
				new AbstractMap.SimpleEntry<>("name 2", "value 2+1"),
				new AbstractMap.SimpleEntry<>("name 2", "value 2+2")
		);

		Flux<PartEvent> partEvents = body
				.concatMap(entry -> FormPartEvent.create(entry.getKey(), entry.getValue()));

		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(partEvents, null, MediaType.MULTIPART_FORM_DATA, this.response, hints)
				.block(Duration.ofSeconds(5));

		MultiValueMap<String, Part> requestParts = parse(this.response, hints);
		assertThat(requestParts).hasSize(2);

		Part part = requestParts.getFirst("name 1");
		assertThat(part.name()).isEqualTo("name 1");
		assertThat(part.headers().getContentType().isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
		String value = decodeToString(part);
		assertThat(value).isEqualTo("value 1");

		List<Part> parts = requestParts.get("name 2");
		assertThat(parts).hasSize(2);

		part = parts.get(0);
		assertThat(part.name()).isEqualTo("name 2");
		assertThat(part.headers().getContentType().isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
		value = decodeToString(part);
		assertThat(value).isEqualTo("value 2+1");

		part = parts.get(1);
		assertThat(part.name()).isEqualTo("name 2");
		assertThat(part.headers().getContentType().isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
		value = decodeToString(part);
		assertThat(value).isEqualTo("value 2+2");
	}

	@SuppressWarnings("ConstantConditions")
	private String decodeToString(Part part) {
		return StringDecoder.textPlainOnly().decodeToMono(part.content(),
				ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN,
				Collections.emptyMap()).block(Duration.ZERO);
	}

}
