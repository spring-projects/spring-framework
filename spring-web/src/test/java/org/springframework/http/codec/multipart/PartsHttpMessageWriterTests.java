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

package org.springframework.http.codec.multipart;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class PartsHttpMessageWriterTests extends AbstractLeakCheckingTests {

	private final PartsHttpMessageWriter writer = new PartsHttpMessageWriter();

	private final MockServerHttpResponse response = new MockServerHttpResponse(this.bufferFactory);

	@Test
	public void canWrite() {
		assertThat(this.writer.canWrite(
				ResolvableType.forClass(Part.class),
				MediaType.MULTIPART_FORM_DATA)).isTrue();

		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				MediaType.MULTIPART_FORM_DATA)).isFalse();
		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.MULTIPART_FORM_DATA)).isFalse();

		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(Map.class, String.class, Object.class),
				MediaType.MULTIPART_FORM_DATA)).isFalse();
		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				MediaType.APPLICATION_FORM_URLENCODED)).isFalse();
	}

	@Test
	public void writeMultipart() {
		FilePart mockFilePart = mockFilePart();
		HttpHeaders fileHeaders = new HttpHeaders();
		fileHeaders.setContentType(MediaType.IMAGE_JPEG);
		given(mockFilePart.headers()).willReturn(fileHeaders);

		Part mockPart = mockPart();
		given(mockPart.headers()).willReturn(new HttpHeaders());

		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(Flux.just(mockFilePart, mockPart), null, MediaType.MULTIPART_FORM_DATA, this.response, hints).block(Duration.ofSeconds(5));

		MultiValueMap<String, Part> requestParts = parse(hints);
		assertThat(requestParts.size()).isEqualTo(2);

		Part part = requestParts.getFirst("filePart");
		assertThat(part).isInstanceOf(FilePart.class);
		assertThat(part.name()).isEqualTo("filePart");
		assertThat(((FilePart) part).filename()).isEqualTo("file.txt");
		assertThat(part.headers().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);

		part = requestParts.getFirst("part");
		assertThat(part).isInstanceOf(Part.class);
		assertThat(part.name()).isEqualTo("part");
	}

	@Test  // SPR-16376
	public void customContentDisposition() throws IOException {
		FilePart mockFilePart = mockFilePart();
		HttpHeaders fileHeaders = new HttpHeaders();
		fileHeaders.setContentType(MediaType.IMAGE_JPEG);
		fileHeaders.setContentDispositionFormData("fileCustomPart", "file_custom.jpg");
		given(mockFilePart.headers()).willReturn(fileHeaders);

		Part mockPart = mockPart();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_JPEG);
		headers.setContentDispositionFormData("customPart", null);
		given(mockPart.headers()).willReturn(headers);

		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(Flux.just(mockFilePart, mockPart), null, MediaType.MULTIPART_FORM_DATA, this.response, hints).block(Duration.ofSeconds(5));

		MultiValueMap<String, Part> requestParts = parse(hints);
		assertThat(requestParts.size()).isEqualTo(2);

		Part part = requestParts.getFirst("fileCustomPart");
		assertThat(part).isInstanceOf(FilePart.class);
		assertThat(part.name()).isEqualTo("fileCustomPart");
		assertThat(((FilePart) part).filename()).isEqualTo("file_custom.jpg");
		assertThat(part.headers().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);

		part = requestParts.getFirst("customPart");
		assertThat(part).isInstanceOf(Part.class);
		assertThat(part.name()).isEqualTo("customPart");
	}

	private MultiValueMap<String, Part> parse(Map<String, Object> hints) {
		MediaType contentType = this.response.getHeaders().getContentType();
		assertThat(contentType.getParameter("boundary")).as("No boundary found").isNotNull();

		// see if Synchronoss NIO Multipart can read what we wrote
		SynchronossPartHttpMessageReader synchronossReader = new SynchronossPartHttpMessageReader();
		MultipartHttpMessageReader reader = new MultipartHttpMessageReader(synchronossReader);

		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.contentType(MediaType.parseMediaType(contentType.toString()))
				.body(this.response.getBody());

		ResolvableType elementType = ResolvableType.forClassWithGenerics(
				MultiValueMap.class, String.class, Part.class);

		MultiValueMap<String, Part> result = reader.readMono(elementType, request, hints)
				.block(Duration.ofSeconds(5));

		assertThat(result).isNotNull();
		return result;
	}

	@NotNull
	private Part mockPart() {
		Flux<DataBuffer> bufferPublisher2 = Flux.just(
				this.bufferFactory.wrap("Db".getBytes(StandardCharsets.UTF_8)),
				this.bufferFactory.wrap("Ee".getBytes(StandardCharsets.UTF_8))
		);

		Part mockPart = mock(Part.class);
		given(mockPart.content()).willReturn(bufferPublisher2);
		given(mockPart.name()).willReturn("part");
		return mockPart;
	}

	@NotNull
	private FilePart mockFilePart() {
		Flux<DataBuffer> bufferPublisher1 = Flux.just(
				this.bufferFactory.wrap("Aa".getBytes(StandardCharsets.UTF_8)),
				this.bufferFactory.wrap("Bb".getBytes(StandardCharsets.UTF_8)),
				this.bufferFactory.wrap("Cc".getBytes(StandardCharsets.UTF_8))
		);
		FilePart mockFilePart = mock(FilePart.class);
		given(mockFilePart.content()).willReturn(bufferPublisher1);
		given(mockFilePart.filename()).willReturn("file.txt");
		given(mockFilePart.name()).willReturn("filePart");
		return mockFilePart;
	}



}
