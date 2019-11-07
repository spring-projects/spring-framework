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
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
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
import static java.util.Collections.singletonMap;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.ResolvableType.forClassWithGenerics;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

/**
 * Unit tests for {@link SynchronossPartHttpMessageReader}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class SynchronossPartHttpMessageReaderTests {

	private final MultipartHttpMessageReader reader =
			new MultipartHttpMessageReader(new SynchronossPartHttpMessageReader());

	private static final ResolvableType PARTS_ELEMENT_TYPE =
			forClassWithGenerics(MultiValueMap.class, String.class, Part.class);

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

		assertTrue(parts.containsKey("filePart"));
		Part part = parts.getFirst("filePart");
		assertTrue(part instanceof FilePart);
		assertEquals("filePart", part.name());
		assertEquals("foo.txt", ((FilePart) part).filename());
		DataBuffer buffer = DataBufferUtils.join(part.content()).block();
		assertEquals(12, buffer.readableByteCount());
		byte[] byteContent = new byte[12];
		buffer.read(byteContent);
		assertEquals("Lorem Ipsum.", new String(byteContent));

		assertTrue(parts.containsKey("textPart"));
		part = parts.getFirst("textPart");
		assertTrue(part instanceof FormFieldPart);
		assertEquals("textPart", part.name());
		assertEquals("sample-text", ((FormFieldPart) part).value());
	}

	@Test // SPR-16545
	public void transferTo() throws IOException {
		ServerHttpRequest request = generateMultipartRequest();
		MultiValueMap<String, Part> parts = this.reader.readMono(PARTS_ELEMENT_TYPE, request, emptyMap()).block();

		assertNotNull(parts);
		FilePart part = (FilePart) parts.getFirst("filePart");
		assertNotNull(part);

		File dest = File.createTempFile(part.filename(), "multipart");
		part.transferTo(dest).block(Duration.ofSeconds(5));

		assertTrue(dest.exists());
		assertEquals(12, dest.length());
		assertTrue(dest.delete());
	}

	@Test
	public void bodyError() {
		ServerHttpRequest request = generateErrorMultipartRequest();
		StepVerifier.create(this.reader.readMono(PARTS_ELEMENT_TYPE, request, emptyMap())).verifyError();
	}

	@Test
	public void readPartsWithoutDemand() {
		ServerHttpRequest request = generateMultipartRequest();
		Mono<MultiValueMap<String, Part>> parts = this.reader.readMono(PARTS_ELEMENT_TYPE, request, emptyMap());
		ZeroDemandSubscriber subscriber = new ZeroDemandSubscriber();
		parts.subscribe(subscriber);
		subscriber.cancel();
	}

	@Test
	public void gh23768() throws IOException {
		ReadableByteChannel channel = new ClassPathResource("invalid.multipart", getClass()).readableChannel();
		Flux<DataBuffer> body = DataBufferUtils.readByteChannel(() -> channel, new DefaultDataBufferFactory(), 1024);

		MediaType contentType = new MediaType("multipart", "form-data",
				singletonMap("boundary", "NbjrKgjbsaMLdnMxMfDpD6myWomYc0qNX0w"));
		ServerHttpRequest request = MockServerHttpRequest.post("/")
				.contentType(contentType)
				.body(body);

		Mono<MultiValueMap<String, Part>> parts = this.reader.readMono(PARTS_ELEMENT_TYPE, request, emptyMap());

		StepVerifier.create(parts)
				.assertNext(result -> assertTrue(result.isEmpty()))
				.verifyComplete();
	}

	@Test
	public void readTooManyParts() {
		testMultipartExceptions(reader -> reader.setMaxParts(1), ex -> {
			assertEquals(DecodingException.class, ex.getClass());
			assertThat(ex.getMessage(), startsWith("Failure while parsing part[2]"));
			assertEquals("Too many parts (2 allowed)", ex.getCause().getMessage());
		});
	}

	@Test
	public void readFilePartTooBig() {
		testMultipartExceptions(reader -> reader.setMaxDiskUsagePerPart(5), ex -> {
			assertEquals(DecodingException.class, ex.getClass());
			assertThat(ex.getMessage(), startsWith("Failure while parsing part[1]"));
			assertEquals("Part[1] exceeded the disk usage limit of 5 bytes", ex.getCause().getMessage());
		});
	}

	@Test
	public void readPartHeadersTooBig() {
		testMultipartExceptions(reader -> reader.setMaxInMemorySize(1), ex -> {
			assertEquals(DecodingException.class, ex.getClass());
			assertThat(ex.getMessage(), startsWith("Failure while parsing part[1]"));
			assertEquals("Part[1] exceeded the in-memory limit of 1 bytes", ex.getCause().getMessage());
		});
	}

	private void testMultipartExceptions(
			Consumer<SynchronossPartHttpMessageReader> configurer, Consumer<Throwable> assertions) {

		SynchronossPartHttpMessageReader reader = new SynchronossPartHttpMessageReader();
		configurer.accept(reader);
		MultipartHttpMessageReader multipartReader = new MultipartHttpMessageReader(reader);
		StepVerifier.create(multipartReader.readMono(PARTS_ELEMENT_TYPE, generateMultipartRequest(), emptyMap()))
				.consumeErrorWith(assertions)
				.verify();
	}

	private ServerHttpRequest generateMultipartRequest() {
		MultipartBodyBuilder partsBuilder = new MultipartBodyBuilder();
		partsBuilder.part("filePart", new ClassPathResource("org/springframework/http/codec/multipart/foo.txt"));
		partsBuilder.part("textPart", "sample-text");

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

	private static class ZeroDemandSubscriber extends BaseSubscriber<MultiValueMap<String, Part>> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			// Just subscribe without requesting
		}
	}

}
