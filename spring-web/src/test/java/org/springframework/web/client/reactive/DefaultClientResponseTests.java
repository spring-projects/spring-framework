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

package org.springframework.web.client.reactive;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
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
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.codec.BodyExtractors.toMono;

/**
 * @author Arjen Poutsma
 */
public class DefaultClientResponseTests {

	private ClientHttpResponse mockResponse;

	private WebClientStrategies mockWebClientStrategies;

	private DefaultClientResponse defaultClientResponse;


	@Before
	public void createMocks() {
		mockResponse = mock(ClientHttpResponse.class);
		mockWebClientStrategies = mock(WebClientStrategies.class);

		defaultClientResponse = new DefaultClientResponse(mockResponse, mockWebClientStrategies);
	}

	@Test
	public void statusCode() throws Exception {
		HttpStatus status = HttpStatus.CONTINUE;
		when(mockResponse.getStatusCode()).thenReturn(status);

		assertEquals(status, defaultClientResponse.statusCode());
	}

	@Test
	public void header() throws Exception {
		HttpHeaders httpHeaders = new HttpHeaders();
		long contentLength = 42L;
		httpHeaders.setContentLength(contentLength);
		MediaType contentType = MediaType.TEXT_PLAIN;
		httpHeaders.setContentType(contentType);
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 80);
		httpHeaders.setHost(host);
		List<HttpRange> range = Collections.singletonList(HttpRange.createByteRange(0, 42));
		httpHeaders.setRange(range);

		when(mockResponse.getHeaders()).thenReturn(httpHeaders);

		ClientResponse.Headers headers = defaultClientResponse.headers();
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
		when(mockResponse.getHeaders()).thenReturn(httpHeaders);
		when(mockResponse.getBody()).thenReturn(body);

		Set<HttpMessageReader<?>> messageReaders = Collections
				.singleton(new DecoderHttpMessageReader<String>(new StringDecoder()));
		when(mockWebClientStrategies.messageReaders()).thenReturn(messageReaders::stream);

		Mono<String> resultMono = defaultClientResponse.body(toMono(String.class));
		assertEquals("foo", resultMono.block());
	}


}