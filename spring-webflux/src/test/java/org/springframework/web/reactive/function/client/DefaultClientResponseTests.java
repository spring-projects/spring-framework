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

package org.springframework.web.reactive.function.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

/**
 * @author Arjen Poutsma
 * @author Denys Ivano
 */
public class DefaultClientResponseTests {

	private ClientHttpResponse mockResponse;

	private ExchangeStrategies mockExchangeStrategies;

	private DefaultClientResponse defaultClientResponse;


	@BeforeEach
	public void createMocks() {
		mockResponse = mock(ClientHttpResponse.class);
		mockExchangeStrategies = mock(ExchangeStrategies.class);
		defaultClientResponse = new DefaultClientResponse(mockResponse, mockExchangeStrategies, "", "", () -> null);
	}


	@Test
	public void statusCode() {
		HttpStatus status = HttpStatus.CONTINUE;
		given(mockResponse.getStatusCode()).willReturn(status);

		assertThat(defaultClientResponse.statusCode()).isEqualTo(status);
	}

	@Test
	public void rawStatusCode() {
		int status = 999;
		given(mockResponse.getRawStatusCode()).willReturn(status);

		assertThat(defaultClientResponse.rawStatusCode()).isEqualTo(status);
	}

	@Test
	public void header() {
		HttpHeaders httpHeaders = new HttpHeaders();
		long contentLength = 42L;
		httpHeaders.setContentLength(contentLength);
		MediaType contentType = MediaType.TEXT_PLAIN;
		httpHeaders.setContentType(contentType);
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 80);
		httpHeaders.setHost(host);
		List<HttpRange> range = Collections.singletonList(HttpRange.createByteRange(0, 42));
		httpHeaders.setRange(range);

		given(mockResponse.getHeaders()).willReturn(httpHeaders);

		ClientResponse.Headers headers = defaultClientResponse.headers();
		assertThat(headers.contentLength()).isEqualTo(OptionalLong.of(contentLength));
		assertThat(headers.contentType()).isEqualTo(Optional.of(contentType));
		assertThat(headers.asHttpHeaders()).isEqualTo(httpHeaders);
	}

	@Test
	public void cookies() {
		ResponseCookie cookie = ResponseCookie.from("foo", "bar").build();
		MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();
		cookies.add("foo", cookie);

		given(mockResponse.getCookies()).willReturn(cookies);

		assertThat(defaultClientResponse.cookies()).isSameAs(cookies);
	}


	@Test
	public void body() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);
		mockTextPlainResponse(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

		Mono<String> resultMono = defaultClientResponse.body(toMono(String.class));
		assertThat(resultMono.block()).isEqualTo("foo");
	}

	@Test
	public void bodyToMono() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);
		mockTextPlainResponse(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

		Mono<String> resultMono = defaultClientResponse.bodyToMono(String.class);
		assertThat(resultMono.block()).isEqualTo("foo");
	}

	@Test
	public void bodyToMonoTypeReference() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);
		mockTextPlainResponse(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

		Mono<String> resultMono =
				defaultClientResponse.bodyToMono(new ParameterizedTypeReference<String>() {
				});
		assertThat(resultMono.block()).isEqualTo("foo");
	}

	@Test
	public void bodyToFlux() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);
		mockTextPlainResponse(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

		Flux<String> resultFlux = defaultClientResponse.bodyToFlux(String.class);
		Mono<List<String>> result = resultFlux.collectList();
		assertThat(result.block()).isEqualTo(Collections.singletonList("foo"));
	}

	@Test
	public void bodyToFluxTypeReference() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);
		mockTextPlainResponse(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

		Flux<String> resultFlux =
				defaultClientResponse.bodyToFlux(new ParameterizedTypeReference<String>() {
				});
		Mono<List<String>> result = resultFlux.collectList();
		assertThat(result.block()).isEqualTo(Collections.singletonList("foo"));
	}

	@Test
	public void toEntity() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);
		mockTextPlainResponse(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

		ResponseEntity<String> result = defaultClientResponse.toEntity(String.class).block();
		assertThat(result.getBody()).isEqualTo("foo");
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	public void toEntityWithUnknownStatusCode() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer
				= factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		given(mockResponse.getHeaders()).willReturn(httpHeaders);
		given(mockResponse.getStatusCode()).willThrow(new IllegalArgumentException("999"));
		given(mockResponse.getRawStatusCode()).willReturn(999);
		given(mockResponse.getBody()).willReturn(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

		ResponseEntity<String> result = defaultClientResponse.toEntity(String.class).block();
		assertThat(result.getBody()).isEqualTo("foo");
		assertThatIllegalArgumentException().isThrownBy(
				result::getStatusCode);
		assertThat(result.getStatusCodeValue()).isEqualTo(999);
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	public void toEntityTypeReference() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);
		mockTextPlainResponse(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

		ResponseEntity<String> result = defaultClientResponse.toEntity(
				new ParameterizedTypeReference<String>() {
				}).block();
		assertThat(result.getBody()).isEqualTo("foo");
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	public void toEntityList() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);
		mockTextPlainResponse(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

		ResponseEntity<List<String>> result = defaultClientResponse.toEntityList(String.class).block();
		assertThat(result.getBody()).isEqualTo(Collections.singletonList("foo"));
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	public void toEntityListWithUnknownStatusCode() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		given(mockResponse.getHeaders()).willReturn(httpHeaders);
		given(mockResponse.getStatusCode()).willThrow(new IllegalArgumentException("999"));
		given(mockResponse.getRawStatusCode()).willReturn(999);
		given(mockResponse.getBody()).willReturn(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

		ResponseEntity<List<String>> result = defaultClientResponse.toEntityList(String.class).block();
		assertThat(result.getBody()).isEqualTo(Collections.singletonList("foo"));
		assertThatIllegalArgumentException().isThrownBy(
				result::getStatusCode);
		assertThat(result.getStatusCodeValue()).isEqualTo(999);
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	public void toEntityListTypeReference() {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		mockTextPlainResponse(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		given(mockExchangeStrategies.messageReaders()).willReturn(messageReaders);

		ResponseEntity<List<String>> result = defaultClientResponse.toEntityList(
				new ParameterizedTypeReference<String>() {
				}).block();
		assertThat(result.getBody()).isEqualTo(Collections.singletonList("foo"));
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}


	private void mockTextPlainResponse(Flux<DataBuffer> body) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		given(mockResponse.getHeaders()).willReturn(httpHeaders);
		given(mockResponse.getStatusCode()).willReturn(HttpStatus.OK);
		given(mockResponse.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(mockResponse.getBody()).willReturn(body);
	}

}
