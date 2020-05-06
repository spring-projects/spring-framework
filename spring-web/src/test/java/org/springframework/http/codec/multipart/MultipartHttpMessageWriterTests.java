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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
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
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class MultipartHttpMessageWriterTests extends AbstractLeakCheckingTests {

	private final MultipartHttpMessageWriter writer =
			new MultipartHttpMessageWriter(ClientCodecConfigurer.create().getWriters());

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
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				MediaType.APPLICATION_FORM_URLENCODED)).isTrue();

		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(Map.class, String.class, Object.class),
				MediaType.MULTIPART_FORM_DATA)).isFalse();
	}

	@Test
	public void writeMultipartFormData() throws Exception {
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		Resource utf8 = new ClassPathResource("/org/springframework/http/converter/logo.jpg") {
			@Override
			public String getFilename() {
				// SPR-12108
				return "Hall\u00F6le.jpg";
			}
		};

		Flux<DataBuffer> bufferPublisher = Flux.just(
				this.bufferFactory.wrap("Aa".getBytes(StandardCharsets.UTF_8)),
				this.bufferFactory.wrap("Bb".getBytes(StandardCharsets.UTF_8)),
				this.bufferFactory.wrap("Cc".getBytes(StandardCharsets.UTF_8))
		);
		FilePart mockPart = mock(FilePart.class);
		given(mockPart.content()).willReturn(bufferPublisher);
		given(mockPart.filename()).willReturn("file.txt");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("name 1", "value 1");
		bodyBuilder.part("name 2", "value 2+1");
		bodyBuilder.part("name 2", "value 2+2");
		bodyBuilder.part("logo", logo);
		bodyBuilder.part("utf8", utf8);
		bodyBuilder.part("json", new Foo("bar"), MediaType.APPLICATION_JSON);
		bodyBuilder.asyncPart("publisher", Flux.just("foo", "bar", "baz"), String.class);
		bodyBuilder.part("filePublisher", mockPart);
		Mono<MultiValueMap<String, HttpEntity<?>>> result = Mono.just(bodyBuilder.build());

		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(result, null, MediaType.MULTIPART_FORM_DATA, this.response, hints)
				.block(Duration.ofSeconds(5));

		MultiValueMap<String, Part> requestParts = parse(hints);
		assertThat(requestParts.size()).isEqualTo(7);

		Part part = requestParts.getFirst("name 1");
		boolean condition4 = part instanceof FormFieldPart;
		assertThat(condition4).isTrue();
		assertThat(part.name()).isEqualTo("name 1");
		assertThat(((FormFieldPart) part).value()).isEqualTo("value 1");

		List<Part> parts2 = requestParts.get("name 2");
		assertThat(parts2.size()).isEqualTo(2);
		part = parts2.get(0);
		boolean condition3 = part instanceof FormFieldPart;
		assertThat(condition3).isTrue();
		assertThat(part.name()).isEqualTo("name 2");
		assertThat(((FormFieldPart) part).value()).isEqualTo("value 2+1");
		part = parts2.get(1);
		boolean condition2 = part instanceof FormFieldPart;
		assertThat(condition2).isTrue();
		assertThat(part.name()).isEqualTo("name 2");
		assertThat(((FormFieldPart) part).value()).isEqualTo("value 2+2");

		part = requestParts.getFirst("logo");
		boolean condition1 = part instanceof FilePart;
		assertThat(condition1).isTrue();
		assertThat(part.name()).isEqualTo("logo");
		assertThat(((FilePart) part).filename()).isEqualTo("logo.jpg");
		assertThat(part.headers().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
		assertThat(part.headers().getContentLength()).isEqualTo(logo.getFile().length());

		part = requestParts.getFirst("utf8");
		boolean condition = part instanceof FilePart;
		assertThat(condition).isTrue();
		assertThat(part.name()).isEqualTo("utf8");
		assertThat(((FilePart) part).filename()).isEqualTo("Hall\u00F6le.jpg");
		assertThat(part.headers().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
		assertThat(part.headers().getContentLength()).isEqualTo(utf8.getFile().length());

		part = requestParts.getFirst("json");
		assertThat(part.name()).isEqualTo("json");
		assertThat(part.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		String value = decodeToString(part);
		assertThat(value).isEqualTo("{\"bar\":\"bar\"}");

		part = requestParts.getFirst("publisher");
		assertThat(part.name()).isEqualTo("publisher");
		value = decodeToString(part);
		assertThat(value).isEqualTo("foobarbaz");

		part = requestParts.getFirst("filePublisher");
		assertThat(part.name()).isEqualTo("filePublisher");
		assertThat(((FilePart) part).filename()).isEqualTo("file.txt");
		value = decodeToString(part);
		assertThat(value).isEqualTo("AaBbCc");
	}

	@Test // gh-24582
	public void writeMultipartRelated() {

		MediaType mediaType = MediaType.parseMediaType("multipart/related;type=foo");

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("name 1", "value 1");
		bodyBuilder.part("name 2", "value 2");
		Mono<MultiValueMap<String, HttpEntity<?>>> result = Mono.just(bodyBuilder.build());

		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(result, null, mediaType, this.response, hints)
				.block(Duration.ofSeconds(5));

		MediaType contentType = this.response.getHeaders().getContentType();
		assertThat(contentType).isNotNull();
		assertThat(contentType.isCompatibleWith(mediaType)).isTrue();
		assertThat(contentType.getParameter("type")).isEqualTo("foo");
		assertThat(contentType.getParameter("boundary")).isNotEmpty();
		assertThat(contentType.getParameter("charset")).isEqualTo("UTF-8");

		MultiValueMap<String, Part> requestParts = parse(hints);
		assertThat(requestParts.size()).isEqualTo(2);
		assertThat(requestParts.getFirst("name 1").name()).isEqualTo("name 1");
		assertThat(requestParts.getFirst("name 2").name()).isEqualTo("name 2");
	}

	@SuppressWarnings("ConstantConditions")
	private String decodeToString(Part part) {
		return StringDecoder.textPlainOnly().decodeToMono(part.content(),
					ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN,
					Collections.emptyMap()).block(Duration.ZERO);
	}

	@Test  // SPR-16402
	public void singleSubscriberWithResource() throws IOException {
		UnicastProcessor<Resource> processor = UnicastProcessor.create();
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		Mono.just(logo).subscribe(processor);

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.asyncPart("logo", processor, Resource.class);

		Mono<MultiValueMap<String, HttpEntity<?>>> result = Mono.just(bodyBuilder.build());

		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(result, null, MediaType.MULTIPART_FORM_DATA, this.response, hints).block();

		MultiValueMap<String, Part> requestParts = parse(hints);
		assertThat(requestParts.size()).isEqualTo(1);

		Part part = requestParts.getFirst("logo");
		assertThat(part.name()).isEqualTo("logo");
		boolean condition = part instanceof FilePart;
		assertThat(condition).isTrue();
		assertThat(((FilePart) part).filename()).isEqualTo("logo.jpg");
		assertThat(part.headers().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
		assertThat(part.headers().getContentLength()).isEqualTo(logo.getFile().length());
	}

	@Test // SPR-16402
	public void singleSubscriberWithStrings() {
		UnicastProcessor<String> processor = UnicastProcessor.create();
		Flux.just("foo", "bar", "baz").subscribe(processor);

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.asyncPart("name", processor, String.class);

		Mono<MultiValueMap<String, HttpEntity<?>>> result = Mono.just(bodyBuilder.build());

		this.writer.write(result, null, MediaType.MULTIPART_FORM_DATA, this.response, Collections.emptyMap())
				.block(Duration.ofSeconds(5));

		// Make sure body is consumed to avoid leak reports
		this.response.getBodyAsString().block(Duration.ofSeconds(5));
	}

	@Test  // SPR-16376
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

		Map<String, Object> hints = Collections.emptyMap();
		this.writer.write(Mono.just(multipartData), null, MediaType.MULTIPART_FORM_DATA,
				this.response, hints).block();

		MultiValueMap<String, Part> requestParts = parse(hints);
		assertThat(requestParts.size()).isEqualTo(2);

		Part part = requestParts.getFirst("resource");
		boolean condition1 = part instanceof FilePart;
		assertThat(condition1).isTrue();
		assertThat(((FilePart) part).filename()).isEqualTo("spring.jpg");
		assertThat(part.headers().getContentLength()).isEqualTo(logo.getFile().length());

		part = requestParts.getFirst("buffers");
		boolean condition = part instanceof FilePart;
		assertThat(condition).isTrue();
		assertThat(((FilePart) part).filename()).isEqualTo("buffers.jpg");
		assertThat(part.headers().getContentLength()).isEqualTo(logo.getFile().length());
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


	@SuppressWarnings("unused")
	private static class Foo {

		private String bar;

		public Foo() {
		}

		public Foo(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
	}

}
