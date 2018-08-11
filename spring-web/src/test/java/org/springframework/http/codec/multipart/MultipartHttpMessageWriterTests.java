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

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class MultipartHttpMessageWriterTests {

	private final MultipartHttpMessageWriter writer =
			new MultipartHttpMessageWriter(ClientCodecConfigurer.create().getWriters());


	@Test
	public void canWrite() {
		assertTrue(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				MediaType.MULTIPART_FORM_DATA));
		assertTrue(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.MULTIPART_FORM_DATA));

		assertFalse(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(Map.class, String.class, Object.class),
				MediaType.MULTIPART_FORM_DATA));
		assertTrue(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				MediaType.APPLICATION_FORM_URLENCODED));
	}

	@Test
	public void writeMultipart() throws Exception {

		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		Resource utf8 = new ClassPathResource("/org/springframework/http/converter/logo.jpg") {
			@Override
			public String getFilename() {
				// SPR-12108
				return "Hall\u00F6le.jpg";
			}
		};

		Publisher<String> publisher = Flux.just("foo", "bar", "baz");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("name 1", "value 1");
		bodyBuilder.part("name 2", "value 2+1");
		bodyBuilder.part("name 2", "value 2+2");
		bodyBuilder.part("logo", logo);
		bodyBuilder.part("utf8", utf8);
		bodyBuilder.part("json", new Foo("bar"), MediaType.APPLICATION_JSON_UTF8);
		bodyBuilder.asyncPart("publisher", publisher, String.class);
		Mono<MultiValueMap<String, HttpEntity<?>>> result = Mono.just(bodyBuilder.build());

		MockServerHttpResponse response = new MockServerHttpResponse();
		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(result, null, MediaType.MULTIPART_FORM_DATA, response, hints).block(Duration.ofSeconds(5));

		MultiValueMap<String, Part> requestParts = parse(response, hints);
		assertEquals(6, requestParts.size());

		Part part = requestParts.getFirst("name 1");
		assertTrue(part instanceof FormFieldPart);
		assertEquals("name 1", part.name());
		assertEquals("value 1", ((FormFieldPart) part).value());

		List<Part> parts2 = requestParts.get("name 2");
		assertEquals(2, parts2.size());
		part = parts2.get(0);
		assertTrue(part instanceof FormFieldPart);
		assertEquals("name 2", part.name());
		assertEquals("value 2+1", ((FormFieldPart) part).value());
		part = parts2.get(1);
		assertTrue(part instanceof FormFieldPart);
		assertEquals("name 2", part.name());
		assertEquals("value 2+2", ((FormFieldPart) part).value());

		part = requestParts.getFirst("logo");
		assertTrue(part instanceof FilePart);
		assertEquals("logo", part.name());
		assertEquals("logo.jpg", ((FilePart) part).filename());
		assertEquals(MediaType.IMAGE_JPEG, part.headers().getContentType());
		assertEquals(logo.getFile().length(), part.headers().getContentLength());

		part = requestParts.getFirst("utf8");
		assertTrue(part instanceof FilePart);
		assertEquals("utf8", part.name());
		assertEquals("Hall\u00F6le.jpg", ((FilePart) part).filename());
		assertEquals(MediaType.IMAGE_JPEG, part.headers().getContentType());
		assertEquals(utf8.getFile().length(), part.headers().getContentLength());

		part = requestParts.getFirst("json");
		assertEquals("json", part.name());
		assertEquals(MediaType.APPLICATION_JSON_UTF8, part.headers().getContentType());

		String value = StringDecoder.textPlainOnly(false).decodeToMono(part.content(),
				ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN,
				Collections.emptyMap()).block(Duration.ZERO);

		assertEquals("{\"bar\":\"bar\"}", value);

		part = requestParts.getFirst("publisher");
		assertEquals("publisher", part.name());

		value = StringDecoder.textPlainOnly(false).decodeToMono(part.content(),
				ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN,
				Collections.emptyMap()).block(Duration.ZERO);

		assertEquals("foobarbaz", value);
	}

	@Test // SPR-16402
	public void singleSubscriberWithResource() throws IOException {
		UnicastProcessor<Resource> processor = UnicastProcessor.create();
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		Mono.just(logo).subscribe(processor);

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.asyncPart("logo", processor, Resource.class);

		Mono<MultiValueMap<String, HttpEntity<?>>> result = Mono.just(bodyBuilder.build());

		MockServerHttpResponse response = new MockServerHttpResponse();
		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(result, null, MediaType.MULTIPART_FORM_DATA, response, hints).block();

		MultiValueMap<String, Part> requestParts = parse(response, hints);
		assertEquals(1, requestParts.size());

		Part part = requestParts.getFirst("logo");
		assertEquals("logo", part.name());
		assertTrue(part instanceof FilePart);
		assertEquals("logo.jpg", ((FilePart) part).filename());
		assertEquals(MediaType.IMAGE_JPEG, part.headers().getContentType());
		assertEquals(logo.getFile().length(), part.headers().getContentLength());
	}

	@Test // SPR-16402
	public void singleSubscriberWithStrings() {
		UnicastProcessor<String> processor = UnicastProcessor.create();
		Flux.just("foo", "bar", "baz").subscribe(processor);

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.asyncPart("name", processor, String.class);

		Mono<MultiValueMap<String, HttpEntity<?>>> result = Mono.just(bodyBuilder.build());

		MockServerHttpResponse response = new MockServerHttpResponse();
		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(result, null, MediaType.MULTIPART_FORM_DATA, response, hints).block();
	}

	@Test // SPR-16376
	public void customContentDisposition() throws IOException {
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		Flux<DataBuffer> buffers = DataBufferUtils.read(logo, new DefaultDataBufferFactory(), 1024);
		long contentLength = logo.contentLength();

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("resource", logo)
				.headers(h -> h.setContentDispositionFormData("resource", "spring.jpg"));
		bodyBuilder.asyncPart("buffers", buffers, DataBuffer.class)
				.headers(h -> {
					h.setContentDispositionFormData("buffers", "buffers.jpg");
					h.setContentType(MediaType.IMAGE_JPEG);
					h.setContentLength(contentLength);
				});

		MultiValueMap<String, HttpEntity<?>> multipartData = bodyBuilder.build();

		MockServerHttpResponse response = new MockServerHttpResponse();
		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(Mono.just(multipartData), null, MediaType.MULTIPART_FORM_DATA, response, hints).block();

		MultiValueMap<String, Part> requestParts = parse(response, hints);
		assertEquals(2, requestParts.size());

		Part part = requestParts.getFirst("resource");
		assertTrue(part instanceof FilePart);
		assertEquals("spring.jpg", ((FilePart) part).filename());
		assertEquals(logo.getFile().length(), part.headers().getContentLength());

		part = requestParts.getFirst("buffers");
		assertTrue(part instanceof FilePart);
		assertEquals("buffers.jpg", ((FilePart) part).filename());
		assertEquals(logo.getFile().length(), part.headers().getContentLength());
	}

	private MultiValueMap<String, Part> parse(MockServerHttpResponse response, Map<String, Object> hints) {
		MediaType contentType = response.getHeaders().getContentType();
		assertNotNull("No boundary found", contentType.getParameter("boundary"));

		// see if Synchronoss NIO Multipart can read what we wrote
		SynchronossPartHttpMessageReader synchronossReader = new SynchronossPartHttpMessageReader();
		MultipartHttpMessageReader reader = new MultipartHttpMessageReader(synchronossReader);

		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.contentType(MediaType.parseMediaType(contentType.toString()))
				.body(response.getBody());

		ResolvableType elementType = ResolvableType.forClassWithGenerics(
				MultiValueMap.class, String.class, Part.class);

		MultiValueMap<String, Part> result = reader.readMono(elementType, request, hints)
				.block(Duration.ofSeconds(5));

		assertNotNull(result);
		return result;
	}


	private class Foo {

		private String bar;

		public Foo() {
		}

		public Foo(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
	}

}
