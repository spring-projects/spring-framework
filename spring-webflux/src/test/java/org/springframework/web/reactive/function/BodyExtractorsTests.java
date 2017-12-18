/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonView;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.FormHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.codec.multipart.SynchronossPartHttpMessageReader;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.codec.json.Jackson2CodecSupport.JSON_VIEW_HINT;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
public class BodyExtractorsTests {

	private BodyExtractor.Context context;

	private Map<String, Object> hints;


	@Before
	public void createContext() {
		final List<HttpMessageReader<?>> messageReaders = new ArrayList<>();
		messageReaders.add(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		messageReaders.add(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes(true)));
		messageReaders.add(new DecoderHttpMessageReader<>(new Jaxb2XmlDecoder()));
		messageReaders.add(new DecoderHttpMessageReader<>(new Jackson2JsonDecoder()));
		messageReaders.add(new FormHttpMessageReader());
		SynchronossPartHttpMessageReader partReader = new SynchronossPartHttpMessageReader();
		messageReaders.add(partReader);
		messageReaders.add(new MultipartHttpMessageReader(partReader));

		messageReaders.add(new FormHttpMessageReader());

		this.context = new BodyExtractor.Context() {
			@Override
			public List<HttpMessageReader<?>> messageReaders() {
				return messageReaders;
			}

			@Override
			public Optional<ServerHttpResponse> serverResponse() {
				return Optional.empty();
			}

			@Override
			public Map<String, Object> hints() {
				return hints;
			}
		};
		this.hints = new HashMap<String, Object>();
	}


	@Test
	public void toMono() throws Exception {
		BodyExtractor<Mono<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(String.class);

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = MockServerHttpRequest.post("/").body(body);
		Mono<String> result = extractor.extract(request, this.context);

		StepVerifier.create(result)
				.expectNext("foo")
				.expectComplete()
				.verify();
	}

	@Test
	public void toMonoParameterizedTypeReference() throws Exception {
		BodyExtractor<Mono<Map<String, String>>, ReactiveHttpInputMessage> extractor =
				BodyExtractors.toMono(new ParameterizedTypeReference<Map<String, String>>() {});

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("{\"username\":\"foo\",\"password\":\"bar\"}".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = MockServerHttpRequest.post("/").contentType(MediaType.APPLICATION_JSON).body(body);
		Mono<Map<String, String>> result = extractor.extract(request, this.context);

		Map<String, String > expected = new LinkedHashMap<>();
		expected.put("username", "foo");
		expected.put("password", "bar");
		StepVerifier.create(result)
				.expectNext(expected)
				.expectComplete()
				.verify();
	}

	@Test
	public void toMonoWithHints() throws Exception {
		BodyExtractor<Mono<User>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(User.class);
		this.hints.put(JSON_VIEW_HINT, SafeToDeserialize.class);

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("{\"username\":\"foo\",\"password\":\"bar\"}".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body);

		Mono<User> result = extractor.extract(request, this.context);

		StepVerifier.create(result)
				.consumeNextWith(user -> {
					assertEquals("foo", user.getUsername());
					assertNull(user.getPassword());
				})
				.expectComplete()
				.verify();
	}

	@Test // SPR-15758
	public void toMonoWithEmptyBodyAndNoContentType() throws Exception {
		BodyExtractor<Mono<Map<String, String>>, ReactiveHttpInputMessage> extractor =
				BodyExtractors.toMono(new ParameterizedTypeReference<Map<String, String>>() {});

		MockServerHttpRequest request = MockServerHttpRequest.post("/").body(Flux.empty());
		Mono<Map<String, String>> result = extractor.extract(request, this.context);

		StepVerifier.create(result).expectComplete().verify();
	}

	@Test
	public void toFlux() throws Exception {
		BodyExtractor<Flux<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toFlux(String.class);

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = MockServerHttpRequest.post("/").body(body);
		Flux<String> result = extractor.extract(request, this.context);

		StepVerifier.create(result)
				.expectNext("foo")
				.expectComplete()
				.verify();
	}

	@Test
	public void toFluxWithHints() throws Exception {
		BodyExtractor<Flux<User>, ReactiveHttpInputMessage> extractor = BodyExtractors.toFlux(User.class);
		this.hints.put(JSON_VIEW_HINT, SafeToDeserialize.class);

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		String text = "[{\"username\":\"foo\",\"password\":\"bar\"},{\"username\":\"bar\",\"password\":\"baz\"}]";
		DefaultDataBuffer dataBuffer = factory.wrap(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body);

		Flux<User> result = extractor.extract(request, this.context);

		StepVerifier.create(result)
				.consumeNextWith(user -> {
					assertEquals("foo", user.getUsername());
					assertNull(user.getPassword());
				})
				.consumeNextWith(user -> {
					assertEquals("bar", user.getUsername());
					assertNull(user.getPassword());
				})
				.expectComplete()
				.verify();
	}

	@Test
	public void toFluxUnacceptable() throws Exception {
		BodyExtractor<Flux<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toFlux(String.class);

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.contentType(MediaType.APPLICATION_JSON)
				.body(body);

		BodyExtractor.Context emptyContext = new BodyExtractor.Context() {
			@Override
			public List<HttpMessageReader<?>> messageReaders() {
				return Collections.emptyList();
			}

			@Override
			public Optional<ServerHttpResponse> serverResponse() {
				return Optional.empty();
			}

			@Override
			public Map<String, Object> hints() {
				return Collections.emptyMap();
			}
		};

		Flux<String> result = extractor.extract(request, emptyContext);
		StepVerifier.create(result)
				.expectError(UnsupportedMediaTypeException.class)
				.verify();
	}

	@Test
	public void toFormData() throws Exception {
		BodyExtractor<Mono<MultiValueMap<String, String>>, ServerHttpRequest> extractor = BodyExtractors.toFormData();

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		String text = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
		DefaultDataBuffer dataBuffer = factory.wrap(ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(body);

		Mono<MultiValueMap<String, String>> result = extractor.extract(request, this.context);

		StepVerifier.create(result)
				.consumeNextWith(form -> {
					assertEquals("Invalid result", 3, form.size());
					assertEquals("Invalid result", "value 1", form.getFirst("name 1"));
					List<String> values = form.get("name 2");
					assertEquals("Invalid result", 2, values.size());
					assertEquals("Invalid result", "value 2+1", values.get(0));
					assertEquals("Invalid result", "value 2+2", values.get(1));
					assertNull("Invalid result", form.getFirst("name 3"));
				})
				.expectComplete()
				.verify();
	}

	@Test
	public void toParts() throws Exception {
		BodyExtractor<Flux<Part>, ServerHttpRequest> extractor = BodyExtractors.toParts();

		String bodyContents = "-----------------------------9051914041544843365972754266\r\n" +
				"Content-Disposition: form-data; name=\"text\"\r\n" +
				"\r\n" +
				"text default\r\n" +
				"-----------------------------9051914041544843365972754266\r\n" +
				"Content-Disposition: form-data; name=\"file1\"; filename=\"a.txt\"\r\n" +
				"Content-Type: text/plain\r\n" +
				"\r\n" +
				"Content of a.txt.\r\n" +
				"\r\n" +
				"-----------------------------9051914041544843365972754266\r\n" +
				"Content-Disposition: form-data; name=\"file2\"; filename=\"a.html\"\r\n" +
				"Content-Type: text/html\r\n" +
				"\r\n" +
				"<!DOCTYPE html><title>Content of a.html.</title>\r\n" +
				"\r\n" +
				"-----------------------------9051914041544843365972754266--\r\n";

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap(bodyContents.getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.header("Content-Type", "multipart/form-data; boundary=---------------------------9051914041544843365972754266")
				.body(body);

		Flux<Part> result = extractor.extract(request, this.context);

		StepVerifier.create(result)
				.consumeNextWith(part -> {
					assertEquals("text", part.name());
					assertTrue(part instanceof FormFieldPart);
					FormFieldPart formFieldPart = (FormFieldPart) part;
					assertEquals("text default", formFieldPart.value());
				})
				.consumeNextWith(part -> {
					assertEquals("file1", part.name());
					assertTrue(part instanceof FilePart);
					FilePart filePart = (FilePart) part;
					assertEquals("a.txt", filePart.filename());
					assertEquals(MediaType.TEXT_PLAIN, filePart.headers().getContentType());
				})
				.consumeNextWith(part -> {
					assertEquals("file2", part.name());
					assertTrue(part instanceof FilePart);
					FilePart filePart = (FilePart) part;
					assertEquals("a.html", filePart.filename());
					assertEquals(MediaType.TEXT_HTML, filePart.headers().getContentType());
				})
				.expectComplete()
				.verify();
	}

	@Test
	public void toDataBuffers() throws Exception {
		BodyExtractor<Flux<DataBuffer>, ReactiveHttpInputMessage> extractor = BodyExtractors.toDataBuffers();

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = MockServerHttpRequest.post("/").body(body);
		Flux<DataBuffer> result = extractor.extract(request, this.context);

		StepVerifier.create(result)
				.expectNext(dataBuffer)
				.expectComplete()
				.verify();
	}


	interface SafeToDeserialize {}


	@SuppressWarnings("unused")
	private static class User {

		@JsonView(SafeToDeserialize.class)
		private String username;

		private String password;

		public User() {
		}

		public User(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}

}
