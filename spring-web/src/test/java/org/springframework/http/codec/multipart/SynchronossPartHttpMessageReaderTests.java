/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.codec.multipart;

import java.io.File;
import java.time.Duration;
import java.util.Map;

import org.junit.Test;
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

import static java.util.Collections.*;
import static org.junit.Assert.*;
import static org.springframework.core.ResolvableType.*;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.MediaType.*;

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
		assertTrue(this.reader.canRead(
				forClassWithGenerics(MultiValueMap.class, String.class, Part.class),
				MediaType.MULTIPART_FORM_DATA));

		assertFalse(this.reader.canRead(
				forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				MediaType.MULTIPART_FORM_DATA));

		assertFalse(this.reader.canRead(
				forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.MULTIPART_FORM_DATA));

		assertFalse(this.reader.canRead(
				forClassWithGenerics(Map.class, String.class, String.class),
				MediaType.MULTIPART_FORM_DATA));

		assertFalse(this.reader.canRead(
				forClassWithGenerics(MultiValueMap.class, String.class, Part.class),
				MediaType.APPLICATION_FORM_URLENCODED));
	}

	@Test
	public void resolveParts() {
		ServerHttpRequest request = generateMultipartRequest();
		ResolvableType elementType = forClassWithGenerics(MultiValueMap.class, String.class, Part.class);
		MultiValueMap<String, Part> parts = this.reader.readMono(elementType, request, emptyMap()).block();
		assertEquals(2, parts.size());

		assertTrue(parts.containsKey("fooPart"));
		Part part = parts.getFirst("fooPart");
		assertTrue(part instanceof FilePart);
		assertEquals("fooPart", part.name());
		assertEquals("foo.txt", ((FilePart) part).filename());
		DataBuffer buffer = DataBufferUtils.join(part.content()).block();
		assertEquals(12, buffer.readableByteCount());
		byte[] byteContent = new byte[12];
		buffer.read(byteContent);
		assertEquals("Lorem Ipsum.", new String(byteContent));

		assertTrue(parts.containsKey("barPart"));
		part = parts.getFirst("barPart");
		assertTrue(part instanceof FormFieldPart);
		assertEquals("barPart", part.name());
		assertEquals("bar", ((FormFieldPart) part).value());
	}

	@Test // SPR-16545
	public void transferTo() {
		ServerHttpRequest request = generateMultipartRequest();
		ResolvableType elementType = forClassWithGenerics(MultiValueMap.class, String.class, Part.class);
		MultiValueMap<String, Part> parts = this.reader.readMono(elementType, request, emptyMap()).block();

		assertNotNull(parts);
		FilePart part = (FilePart) parts.getFirst("fooPart");
		assertNotNull(part);

		File dest = new File(System.getProperty("java.io.tmpdir") + "/" + part.filename());
		part.transferTo(dest).block(Duration.ofSeconds(5));

		assertTrue(dest.exists());
		assertEquals(12, dest.length());
		assertTrue(dest.delete());
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
