/*
 * Copyright 2002-2024 the original author or authors.
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

/**
 * @author Arjen Poutsma
 * @author Denys Ivano
 */
class DefaultClientResponseTests {

	private static final ParameterizedTypeReference<String> STRING_TYPE = new ParameterizedTypeReference<>() {};


	private final ClientHttpResponse mockResponse = mock();

	private final HttpHeaders httpHeaders = new HttpHeaders();

	private final ExchangeStrategies mockExchangeStrategies = mock();

	@Nullable
	private HttpRequest httpRequest = null;

	private DefaultClientResponse defaultClientResponse;


	@BeforeEach
	void configureMocks() {
		given(mockResponse.getHeaders()).willReturn(this.httpHeaders);
		defaultClientResponse = new DefaultClientResponse(mockResponse, mockExchangeStrategies, "", "", () -> httpRequest);
	}


	@Test
	void statusCode() {
		HttpStatus status = HttpStatus.CONTINUE;
		given(mockResponse.getStatusCode()).willReturn(status);

		assertThat(defaultClientResponse.statusCode()).isEqualTo(status);
	}

	@Test
	void header() {
		long contentLength = 42L;
		httpHeaders.setContentLength(contentLength);
		MediaType contentType = MediaType.TEXT_PLAIN;
		httpHeaders.setContentType(contentType);
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 80);
		httpHeaders.setHost(host);
		httpHeaders.setRange(List.of(HttpRange.createByteRange(0, 42)));

		given(mockResponse.getHeaders()).willReturn(httpHeaders);

		ClientResponse.Headers headers = defaultClientResponse.headers();
		assertThat(headers.contentLength()).isEqualTo(OptionalLong.of(contentLength));
		assertThat(headers.contentType()).contains(contentType);
		assertThat(headers.asHttpHeaders()).isEqualTo(httpHeaders);
	}

	@Test
	void cookies() {
		ResponseCookie cookie = ResponseCookie.from("foo", "bar").build();
		MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();
		cookies.add("foo", cookie);

		given(mockResponse.getCookies()).willReturn(cookies);

		assertThat(defaultClientResponse.cookies()).isSameAs(cookies);
	}


	@Test
	void body() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		mockTextPlainResponse(Flux.just(dataBuffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

		Mono<String> resultMono = defaultClientResponse.body(toMono(String.class));
		assertThat(resultMono.block()).isEqualTo("foo");
	}

	@Test
	void bodyToMono() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		mockTextPlainResponse(Flux.just(dataBuffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

		Mono<String> resultMono = defaultClientResponse.bodyToMono(String.class);
		assertThat(resultMono.block()).isEqualTo("foo");
	}

	@Test
	void bodyToMonoTypeReference() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		mockTextPlainResponse(Flux.just(dataBuffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

		Mono<String> resultMono = defaultClientResponse.bodyToMono(STRING_TYPE);
		assertThat(resultMono.block()).isEqualTo("foo");
	}

	@Test
	void bodyToFlux() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		mockTextPlainResponse(Flux.just(dataBuffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

		Flux<String> resultFlux = defaultClientResponse.bodyToFlux(String.class);
		Mono<List<String>> result = resultFlux.collectList();
		assertThat(result.block()).containsExactly("foo");
	}

	@Test
	void bodyToFluxTypeReference() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		mockTextPlainResponse(Flux.just(dataBuffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

		Flux<String> resultFlux = defaultClientResponse.bodyToFlux(STRING_TYPE);
		Mono<List<String>> result = resultFlux.collectList();
		assertThat(result.block()).containsExactly("foo");
	}

	@Test
	@SuppressWarnings("deprecation")
	void toEntity() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		mockTextPlainResponse(Flux.just(dataBuffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

		ResponseEntity<String> result = defaultClientResponse.toEntity(String.class).block();
		assertThat(result.getBody()).isEqualTo("foo");
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	@SuppressWarnings("deprecation")
	void toEntityWithUnknownStatusCode() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		given(mockResponse.getHeaders()).willReturn(httpHeaders);
		given(mockResponse.getStatusCode()).willReturn(HttpStatusCode.valueOf(999));
		given(mockResponse.getBody()).willReturn(Flux.just(dataBuffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

		ResponseEntity<String> result = defaultClientResponse.toEntity(String.class).block();
		assertThat(result.getBody()).isEqualTo("foo");
		assertThat(result.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(999));
		assertThat(result.getStatusCodeValue()).isEqualTo(999);
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	@SuppressWarnings("deprecation")
	void toEntityTypeReference() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		mockTextPlainResponse(Flux.just(dataBuffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

		ResponseEntity<String> result = defaultClientResponse.toEntity(STRING_TYPE).block();
		assertThat(result.getBody()).isEqualTo("foo");
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	@SuppressWarnings("deprecation")
	void toEntityList() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		mockTextPlainResponse(Flux.just(dataBuffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

		ResponseEntity<List<String>> result = defaultClientResponse.toEntityList(String.class).block();
		assertThat(result.getBody()).containsExactly("foo");
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	@SuppressWarnings("deprecation")
	void toEntityListWithUnknownStatusCode() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		given(mockResponse.getHeaders()).willReturn(httpHeaders);
		given(mockResponse.getStatusCode()).willReturn(HttpStatusCode.valueOf(999));
		given(mockResponse.getBody()).willReturn(Flux.just(dataBuffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

		ResponseEntity<List<String>> result = defaultClientResponse.toEntityList(String.class).block();
		assertThat(result.getBody()).containsExactly("foo");
		assertThat(result.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(999));
		assertThat(result.getStatusCodeValue()).isEqualTo(999);
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	@SuppressWarnings("deprecation")
	void toEntityListTypeReference() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

		mockTextPlainResponse(Flux.just(dataBuffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes())));

		ResponseEntity<List<String>> result = defaultClientResponse.toEntityList(STRING_TYPE).block();
		assertThat(result.getBody()).containsExactly("foo");
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
		assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
	}

	@Test
	void createException() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		given(mockResponse.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		given(mockResponse.getBody()).willReturn(Flux.just(dataBuffer));

		httpRequest = new HttpRequest() {
			@Override
			public HttpMethod getMethod() {
				return HttpMethod.valueOf("UNKNOWN");
			}

			@Override
			public URI getURI() {
				return URI.create("https://user:pass@example.org:9999/app/path?token=secret#fragment");
			}

			@Override
			public HttpHeaders getHeaders() {
				return HttpHeaders.EMPTY;
			}

			@Override
			public Map<String, Object> getAttributes() {
				return Collections.emptyMap();
			}
		};

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(new ByteArrayDecoder())));

		Mono<WebClientResponseException> resultMono = defaultClientResponse.createException();
		WebClientResponseException exception = resultMono.block();
		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.getMessage()).isEqualTo("404 Not Found from UNKNOWN https://example.org:9999/app/path");
		assertThat(exception.getHeaders()).containsExactly(entry("Content-Type", List.of("text/plain")));
		assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(bytes);
	}

	@Test
	@SuppressWarnings("unchecked")
	void createExceptionAndDecodeContent() {
		byte[] bytes = "{\"name\":\"Jason\"}".getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		given(mockResponse.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		given(mockResponse.getBody()).willReturn(Flux.just(buffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(List.of(
				new DecoderHttpMessageReader<>(new ByteArrayDecoder()),
				new DecoderHttpMessageReader<>(new Jackson2JsonDecoder())));

		WebClientResponseException ex = defaultClientResponse.createException().block();
		assertThat(ex.getResponseBodyAs(Map.class)).containsExactly(entry("name", "Jason"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void createExceptionAndDecodeWithoutContent() {
		byte[] bytes = new byte[0];
		DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		given(mockResponse.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		given(mockResponse.getBody()).willReturn(Flux.just(buffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(List.of(
				new DecoderHttpMessageReader<>(new ByteArrayDecoder()),
				new DecoderHttpMessageReader<>(new Jackson2JsonDecoder())));

		WebClientResponseException ex = defaultClientResponse.createException().block();
		assertThat(ex.getResponseBodyAs(Map.class)).isNull();
	}

	@Test
	void createError() {
		byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
		DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		given(mockResponse.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		given(mockResponse.getBody()).willReturn(Flux.just(dataBuffer));

		given(mockExchangeStrategies.messageReaders()).willReturn(
				List.of(new DecoderHttpMessageReader<>(new ByteArrayDecoder())));

		Mono<String> resultMono = defaultClientResponse.createError();
		StepVerifier.create(resultMono)
				.consumeErrorWith(t -> {
					assertThat(t).isInstanceOf(WebClientResponseException.class);
					WebClientResponseException exception = (WebClientResponseException) t;
					assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
					assertThat(exception.getMessage()).isEqualTo("404 Not Found");
					assertThat(exception.getHeaders()).containsExactly(entry("Content-Type",List.of("text/plain")));
					assertThat(exception.getResponseBodyAsByteArray()).isEqualTo(bytes);
				})
				.verify();
	}


	private void mockTextPlainResponse(Flux<DataBuffer> body) {
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		given(mockResponse.getStatusCode()).willReturn(HttpStatus.OK);
		given(mockResponse.getBody()).willReturn(body);
	}

}
