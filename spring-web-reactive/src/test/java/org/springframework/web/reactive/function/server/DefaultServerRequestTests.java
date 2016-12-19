/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

/**
 * @author Arjen Poutsma
 */
public class DefaultServerRequestTests {

	private ServerHttpRequest mockRequest;

	private ServerHttpResponse mockResponse;

	private ServerWebExchange mockExchange;

	private HandlerStrategies mockHandlerStrategies;

	private DefaultServerRequest defaultRequest;

	@Before
	public void createMocks() {
		mockRequest = mock(ServerHttpRequest.class);
		mockResponse = mock(ServerHttpResponse.class);

		mockExchange = mock(ServerWebExchange.class);
		when(mockExchange.getRequest()).thenReturn(mockRequest);
		when(mockExchange.getResponse()).thenReturn(mockResponse);
		mockHandlerStrategies = mock(HandlerStrategies.class);

		defaultRequest = new DefaultServerRequest(mockExchange, mockHandlerStrategies);
	}

	@Test
	public void method() throws Exception {
		HttpMethod method = HttpMethod.HEAD;
		when(mockRequest.getMethod()).thenReturn(method);

		assertEquals(method, defaultRequest.method());
	}

	@Test
	public void uri() throws Exception {
		URI uri = URI.create("https://example.com");
		when(mockRequest.getURI()).thenReturn(uri);

		assertEquals(uri, defaultRequest.uri());
	}

	@Test
	public void attribute() throws Exception {
		when(mockExchange.getAttribute("foo")).thenReturn(Optional.of("bar"));

		assertEquals(Optional.of("bar"), defaultRequest.attribute("foo"));
	}

	@Test
	public void queryParams() throws Exception {
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.set("foo", "bar");
		when(mockRequest.getQueryParams()).thenReturn(queryParams);

		assertEquals(Optional.of("bar"), defaultRequest.queryParam("foo"));
	}

	@Test
	public void pathVariable() throws Exception {
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		when(mockExchange.getAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(Optional.of(pathVariables));

		assertEquals("bar", defaultRequest.pathVariable("foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void pathVariableNotFound() throws Exception {
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		when(mockExchange.getAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(Optional.of(pathVariables));

		assertEquals("bar", defaultRequest.pathVariable("baz"));
	}

	@Test
	public void pathVariables() throws Exception {
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		when(mockExchange.getAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(Optional.of(pathVariables));

		assertEquals(pathVariables, defaultRequest.pathVariables());
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

		when(mockRequest.getHeaders()).thenReturn(httpHeaders);

		ServerRequest.Headers headers = defaultRequest.headers();
		assertEquals(accept, headers.accept());
		assertEquals(acceptCharset, headers.acceptCharset());
		assertEquals(OptionalLong.of(contentLength), headers.contentLength());
		assertEquals(Optional.of(contentType), headers.contentType());
		assertEquals(httpHeaders, headers.asHttpHeaders());
	}

	@Test
	public void body() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(mockRequest.getHeaders()).thenReturn(httpHeaders);
		when(mockRequest.getBody()).thenReturn(body);

		Set<HttpMessageReader<?>> messageReaders = Collections
				.singleton(new DecoderHttpMessageReader<String>(new StringDecoder()));
		when(mockHandlerStrategies.messageReaders()).thenReturn(messageReaders::stream);

		Mono<String> resultMono = defaultRequest.body(toMono(String.class));
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
		when(mockRequest.getHeaders()).thenReturn(httpHeaders);
		when(mockRequest.getBody()).thenReturn(body);

		Set<HttpMessageReader<?>> messageReaders = Collections
				.singleton(new DecoderHttpMessageReader<String>(new StringDecoder()));
		when(mockHandlerStrategies.messageReaders()).thenReturn(messageReaders::stream);

		Mono<String> resultMono = defaultRequest.bodyToMono(String.class);
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
		when(mockRequest.getHeaders()).thenReturn(httpHeaders);
		when(mockRequest.getBody()).thenReturn(body);

		Set<HttpMessageReader<?>> messageReaders = Collections
				.singleton(new DecoderHttpMessageReader<String>(new StringDecoder()));
		when(mockHandlerStrategies.messageReaders()).thenReturn(messageReaders::stream);

		Flux<String> resultFlux = defaultRequest.bodyToFlux(String.class);
		Mono<List<String>> result = resultFlux.collectList();
		assertEquals(Collections.singletonList("foo"), result.block());
	}

}