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

import java.io.File;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.test.MockClientHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.util.MultiValueMap;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClassWithGenerics;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

/**
 * Unit tests for {@link SynchronossPartHttpMessageReader}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class SynchronossPartHttpMessageReaderTests {

	private final MultipartHttpMessageReader reader =
			new MultipartHttpMessageReader(new SynchronossPartHttpMessageReader());


	@Test
	public void canRead() {
		assertThat(this.reader.canRead(
				forClassWithGenerics(MultiValueMap.class, String.class, Part.class),
				MediaType.MULTIPART_FORM_DATA)).isTrue();

		assertThat(this.reader.canRead(
				forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				MediaType.MULTIPART_FORM_DATA)).isFalse();

		assertThat(this.reader.canRead(
				forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.MULTIPART_FORM_DATA)).isFalse();

		assertThat(this.reader.canRead(
				forClassWithGenerics(Map.class, String.class, String.class),
				MediaType.MULTIPART_FORM_DATA)).isFalse();

		assertThat(this.reader.canRead(
				forClassWithGenerics(MultiValueMap.class, String.class, Part.class),
				MediaType.APPLICATION_FORM_URLENCODED)).isFalse();
	}

	@Test
	public void resolveParts() {
		ServerHttpRequest request = generateMultipartRequest();
		ResolvableType elementType = forClassWithGenerics(MultiValueMap.class, String.class, Part.class);
		MultiValueMap<String, Part> parts = this.reader.readMono(elementType, request, emptyMap()).block();
		assertThat(parts.size()).isEqualTo(2);

		assertThat(parts.containsKey("fooPart")).isTrue();
		Part part = parts.getFirst("fooPart");
		boolean condition1 = part instanceof FilePart;
		assertThat(condition1).isTrue();
		assertThat(part.name()).isEqualTo("fooPart");
		assertThat(((FilePart) part).filename()).isEqualTo("foo.txt");
		DataBuffer buffer = DataBufferUtils.join(part.content()).block();
		assertThat(buffer.readableByteCount()).isEqualTo(12);
		byte[] byteContent = new byte[12];
		buffer.read(byteContent);
		assertThat(new String(byteContent)).isEqualTo("Lorem Ipsum.");

		assertThat(parts.containsKey("barPart")).isTrue();
		part = parts.getFirst("barPart");
		boolean condition = part instanceof FormFieldPart;
		assertThat(condition).isTrue();
		assertThat(part.name()).isEqualTo("barPart");
		assertThat(((FormFieldPart) part).value()).isEqualTo("bar");
	}

	@Test // SPR-16545
	public void transferTo() {
		ServerHttpRequest request = generateMultipartRequest();
		ResolvableType elementType = forClassWithGenerics(MultiValueMap.class, String.class, Part.class);
		MultiValueMap<String, Part> parts = this.reader.readMono(elementType, request, emptyMap()).block();

		assertThat(parts).isNotNull();
		FilePart part = (FilePart) parts.getFirst("fooPart");
		assertThat(part).isNotNull();

		File dest = new File(System.getProperty("java.io.tmpdir") + "/" + part.filename());
		part.transferTo(dest).block(Duration.ofSeconds(5));

		assertThat(dest.exists()).isTrue();
		assertThat(dest.length()).isEqualTo(12);
		assertThat(dest.delete()).isTrue();
	}

	@Test
	public void bodyError() {
		ServerHttpRequest request = generateErrorMultipartRequest();
		ResolvableType elementType = forClassWithGenerics(MultiValueMap.class, String.class, Part.class);
		StepVerifier.create(this.reader.readMono(elementType, request, emptyMap())).verifyError();
	}


	private ServerHttpRequest generateMultipartRequest() {

		MultipartBodyBuilder partsBuilder = new MultipartBodyBuilder();
		partsBuilder.part("fooPart", new ClassPathResource("org/springframework/http/codec/multipart/foo.txt"));
		partsBuilder.part("barPart", "bar");

		MockClientHttpRequest outputMessage = new MockClientHttpRequest(HttpMethod.POST, "/");
		new MultipartHttpMessageWriter()
				.write(Mono.just(partsBuilder.build()), null, MediaType.MULTIPART_FORM_DATA, outputMessage, null)
				.block(Duration.ofSeconds(5));

		return MockServerHttpRequest.post("/")
				.contentType(outputMessage.getHeaders().getContentType())
				.body(outputMessage.getBody());
	}

	private ServerHttpRequest generateErrorMultipartRequest() {
		return MockServerHttpRequest.post("/")
				.header(CONTENT_TYPE, MULTIPART_FORM_DATA.toString())
				.body(Flux.just(new DefaultDataBufferFactory().wrap("invalid content".getBytes())));
	}

}
