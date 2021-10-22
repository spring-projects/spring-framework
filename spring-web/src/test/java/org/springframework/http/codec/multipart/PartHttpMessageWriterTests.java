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

package org.springframework.http.codec.multipart;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.http.codec.multipart.MultipartHttpMessageWriterTests.parse;

/**
 * Unit tests for {@link PartHttpMessageWriter}.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class PartHttpMessageWriterTests extends AbstractLeakCheckingTests {

	private final PartHttpMessageWriter writer = new PartHttpMessageWriter();

	private final MockServerHttpResponse response = new MockServerHttpResponse(this.bufferFactory);


	@Test
	public void canWrite() {
		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				MediaType.MULTIPART_FORM_DATA)).isTrue();
		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.MULTIPART_FORM_DATA)).isTrue();
		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				MediaType.MULTIPART_MIXED)).isTrue();
		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				MediaType.MULTIPART_RELATED)).isTrue();

		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(Map.class, String.class, Object.class),
				MediaType.MULTIPART_FORM_DATA)).isFalse();
	}

	@Test
	void write() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		Part textPart = mock(Part.class);
		given(textPart.name()).willReturn("text part");
		given(textPart.headers()).willReturn(headers);
		given(textPart.content()).willReturn(Flux.just(
				this.bufferFactory.wrap("text1".getBytes(StandardCharsets.UTF_8)),
				this.bufferFactory.wrap("text2".getBytes(StandardCharsets.UTF_8))));

		FilePart filePart = mock(FilePart.class);
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
		assertThat(requestParts.size()).isEqualTo(2);

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

	@SuppressWarnings("ConstantConditions")
	private String decodeToString(Part part) {
		return StringDecoder.textPlainOnly().decodeToMono(part.content(),
				ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN,
				Collections.emptyMap()).block(Duration.ZERO);
	}

}
