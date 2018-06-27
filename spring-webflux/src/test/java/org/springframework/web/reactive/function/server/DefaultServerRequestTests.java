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

package org.springframework.web.reactive.function.server;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import static org.junit.Assert.*;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

/**
 * @author Arjen Poutsma
 */
public class DefaultServerRequestTests {

	private final List<HttpMessageReader<?>> messageReaders = Collections.singletonList(
			new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));


	@Test
	public void method() throws Exception {
		HttpMethod method = HttpMethod.HEAD;
		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest.method(method, "http://example.com")),
				this.messageReaders);

		assertEquals(method, request.method());
	}

	@Test
	public void uri() throws Exception {
		URI uri = URI.create("https://example.com");

		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, uri)),
				this.messageReaders);

		assertEquals(uri, request.uri());
	}

	@Test
	public void uriBuilder() throws Exception {
		URI uri = new URI("http", "localhost", "/path", "a=1", null);
		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, uri)),
				this.messageReaders);


		URI result = request.uriBuilder().build();
		assertEquals("http", result.getScheme());
		assertEquals("localhost", result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("/path", result.getPath());
		assertEquals("a=1", result.getQuery());
	}

	@Test
	public void attribute() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.method(HttpMethod.GET, "http://example.com"));
		exchange.getAttributes().put("foo", "bar");

		DefaultServerRequest request = new DefaultServerRequest(exchange, messageReaders);

		assertEquals(Optional.of("bar"), request.attribute("foo"));
	}

	@Test
	public void queryParams() throws Exception {
		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, "http://example.com?foo=bar")),
				this.messageReaders);

		assertEquals(Optional.of("bar"), request.queryParam("foo"));
	}

	@Test
	public void emptyQueryParam() throws Exception {
		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, "http://example.com?foo")),
				this.messageReaders);

		assertEquals(Optional.of(""), request.queryParam("foo"));
	}

	@Test
	public void absentQueryParam() throws Exception {
		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, "http://example.com?foo")),
				this.messageReaders);

		assertEquals(Optional.empty(), request.queryParam("bar"));
	}

	@Test
	public void pathVariable() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://example.com"));
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		exchange.getAttributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(exchange, messageReaders);

		assertEquals("bar", request.pathVariable("foo"));
	}


	@Test(expected = IllegalArgumentException.class)
	public void pathVariableNotFound() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://example.com"));
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		exchange.getAttributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(exchange, messageReaders);

		request.pathVariable("baz");
	}

	@Test
	public void pathVariables() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://example.com"));
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		exchange.getAttributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

		DefaultServerRequest request = new DefaultServerRequest(exchange, messageReaders);

		assertEquals(pathVariables, request.pathVariables());
	}

	@Test
	public void header() throws Exception {
		HttpHeaders httpHeaders = new HttpHeaders();
		List<MediaType> accept =
				Collections.singletonList(MediaType.APPLICATION_JSON);
		httpHeaders.setAccept(accept);
		List<Charset> acceptCharset = Collections.singletonList(StandardCharsets.UTF_8);
		httpHeaders.setAcceptCharset(acceptCharset);
		long contentLength = 42L;
		httpHeaders.setContentLength(contentLength);
		MediaType contentType = MediaType.TEXT_PLAIN;
		httpHeaders.setContentType(contentType);
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 80);
		httpHeaders.setHost(host);
		List<HttpRange> range = Collections.singletonList(HttpRange.createByteRange(0, 42));
		httpHeaders.setRange(range);

		DefaultServerRequest request = new DefaultServerRequest(
				MockServerWebExchange.from(MockServerHttpRequest
						.method(HttpMethod.GET, "http://example.com?foo=bar")
						.headers(httpHeaders)),
				this.messageReaders);

		ServerRequest.Headers headers = request.headers();
		assertEquals(accept, headers.accept());
		assertEquals(acceptCharset, headers.acceptCharset());
		assertEquals(OptionalLong.of(contentLength), headers.contentLength());
		assertEquals(Optional.of(contentType), headers.contentType());
		assertEquals(httpHeaders, headers.asHttpHeaders());
	}

	@Test
	public void cookies() {
		HttpCookie cookie = new HttpCookie("foo", "bar");
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.method(HttpMethod.GET, "http://example.com").cookie(cookie));

		DefaultServerRequest request = new DefaultServerRequest(exchange, messageReaders);

		MultiValueMap<String, HttpCookie> expected = new LinkedMultiValueMap<>();
		expected.add("foo", cookie);

		assertEquals(expected, request.cookies());

	}

	@Test
	public void body() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);

		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "http://example.com?foo=bar")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), messageReaders);

		Mono<String> resultMono = request.body(toMono(String.class));
		assertEquals("foo", resultMono.block());
	}

	@Test
	public void bodyToMono() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "http://example.com?foo=bar")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), messageReaders);

		Mono<String> resultMono = request.bodyToMono(String.class);
		assertEquals("foo", resultMono.block());
	}

	@Test
	public void bodyToMonoParameterizedTypeReference() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "http://example.com?foo=bar")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), messageReaders);

		ParameterizedTypeReference<String> typeReference = new ParameterizedTypeReference<String>() {};
		Mono<String> resultMono = request.bodyToMono(typeReference);
		assertEquals("foo", resultMono.block());
	}

	@Test
	public void bodyToFlux() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "http://example.com?foo=bar")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), messageReaders);

		Flux<String> resultFlux = request.bodyToFlux(String.class);
		assertEquals(Collections.singletonList("foo"), resultFlux.collectList().block());
	}

	@Test
	public void bodyToFluxParameterizedTypeReference() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "http://example.com?foo=bar")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), messageReaders);

		ParameterizedTypeReference<String> typeReference = new ParameterizedTypeReference<String>() {};
		Flux<String> resultFlux = request.bodyToFlux(typeReference);
		assertEquals(Collections.singletonList("foo"), resultFlux.collectList().block());
	}

	@Test
	public void bodyUnacceptable() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "http://example.com?foo=bar")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Flux<String> resultFlux = request.bodyToFlux(String.class);
		StepVerifier.create(resultFlux)
				.expectError(UnsupportedMediaTypeStatusException.class)
				.verify();
	}

	@Test
	public void formData() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo=bar&baz=qux".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "http://example.com")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<MultiValueMap<String, String>> resultData = request.formData();
		StepVerifier.create(resultData)
				.consumeNextWith(formData -> {
					assertEquals(2, formData.size());
					assertEquals("bar", formData.getFirst("foo"));
					assertEquals("qux", formData.getFirst("baz"));
				})
				.verifyComplete();
	}

	@Test
	public void multipartData() throws Exception {
		String data = "--12345\r\n" +
				"Content-Disposition: form-data; name=\"foo\"\r\n" +
				"\r\n" +
				"bar\r\n" +
				"--12345\r\n" +
				"Content-Disposition: form-data; name=\"baz\"\r\n" +
				"\r\n" +
				"qux\r\n" +
				"--12345--\r\n";
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=12345");
		MockServerHttpRequest mockRequest = MockServerHttpRequest
				.method(HttpMethod.GET, "http://example.com")
				.headers(httpHeaders)
				.body(body);
		DefaultServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		Mono<MultiValueMap<String, Part>> resultData = request.multipartData();
		StepVerifier.create(resultData)
				.consumeNextWith(formData -> {
					assertEquals(2, formData.size());

					Part part = formData.getFirst("foo");
					assertTrue(part instanceof FormFieldPart);
					FormFieldPart formFieldPart = (FormFieldPart) part;
					assertEquals("bar", formFieldPart.value());

					part = formData.getFirst("baz");
					assertTrue(part instanceof FormFieldPart);
					formFieldPart = (FormFieldPart) part;
					assertEquals("qux", formFieldPart.value());
				})
				.verifyComplete();
	}
}
