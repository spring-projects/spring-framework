/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;

/**
 * Represents an HTTP response, as returned by {@link WebClient} and also
 * {@link ExchangeFunction}. Provides access to the response status and headers,
 * and also methods to consume the response body.
 *
 * <p><strong>NOTE:</strong> When given access to a {@link ClientResponse},
 * through the {@code WebClient}
 * {@link WebClient.RequestHeadersSpec#exchange() exchange()} method,
 * you must always use one of the body or toEntity methods to ensure resources
 * are released and avoid potential issues with HTTP connection pooling.
 * You can use {@code bodyToMono(Void.class)} if no response content is
 * expected. However keep in mind that if the response does have content, the
 * connection will be closed and will not be placed back in the pool.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ClientResponse {

	/**
	 * Return the status code of this response.
	 * @return the status as an HttpStatus enum value
	 * @throws IllegalArgumentException in case of an unknown HTTP status code
	 * @see HttpStatus#valueOf(int)
	 */
	HttpStatus statusCode();

	/**
	 * Return the (potentially non-standard) status code of this response.
	 * @return the status as an integer
	 * @since 5.1
	 * @see #statusCode()
	 * @see HttpStatus#resolve(int)
	 */
	int rawStatusCode();

	/**
	 * Return the headers of this response.
	 */
	Headers headers();

	/**
	 * Return cookies of this response.
	 */
	MultiValueMap<String, ResponseCookie> cookies();

	/**
	 * Return the strategies used to convert the body of this response.
	 */
	ExchangeStrategies strategies();

	/**
	 * Extract the body with the given {@code BodyExtractor}.
	 * @param extractor the {@code BodyExtractor} that reads from the response
	 * @param <T> the type of the body returned
	 * @return the extracted body
	 */
	<T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor);

	/**
	 * Extract the body to a {@code Mono}.
	 * @param elementClass the class of element in the {@code Mono}
	 * @param <T> the element type
	 * @return a mono containing the body of the given type {@code T}
	 */
	<T> Mono<T> bodyToMono(Class<? extends T> elementClass);

	/**
	 * Extract the body to a {@code Mono}.
	 * @param typeReference a type reference describing the expected response body type
	 * @param <T> the element type
	 * @return a mono containing the body of the given type {@code T}
	 */
	<T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference);

	/**
	 * Extract the body to a {@code Flux}.
	 * @param elementClass the class of element in the {@code Flux}
	 * @param <T> the element type
	 * @return a flux containing the body of the given type {@code T}
	 */
	<T> Flux<T> bodyToFlux(Class<? extends T> elementClass);

	/**
	 * Extract the body to a {@code Flux}.
	 * @param typeReference a type reference describing the expected response body type
	 * @param <T> the element type
	 * @return a flux containing the body of the given type {@code T}
	 */
	<T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference);

	/**
	 * Return this response as a delayed {@code ResponseEntity}.
	 * @param bodyType the expected response body type
	 * @param <T> response body type
	 * @return {@code Mono} with the {@code ResponseEntity}
	 */
	<T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyType);

	/**
	 * Return this response as a delayed {@code ResponseEntity}.
	 * @param typeReference a type reference describing the expected response body type
	 * @param <T> response body type
	 * @return {@code Mono} with the {@code ResponseEntity}
	 */
	<T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> typeReference);

	/**
	 * Return this response as a delayed list of {@code ResponseEntity}s.
	 * @param elementType the expected response body list element type
	 * @param <T> the type of elements in the list
	 * @return {@code Mono} with the list of {@code ResponseEntity}s
	 */
	<T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementType);

	/**
	 * Return this response as a delayed list of {@code ResponseEntity}s.
	 * @param typeReference a type reference describing the expected response body type
	 * @param <T> the type of elements in the list
	 * @return {@code Mono} with the list of {@code ResponseEntity}s
	 */
	<T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> typeReference);


	// Static builder methods

	/**
	 * Create a builder with the status, headers, and cookies of the given response.
	 * @param other the response to copy the status, headers, and cookies from
	 * @return the created builder
	 */
	static Builder from(ClientResponse other) {
		return new DefaultClientResponseBuilder(other);
	}

	/**
	 * Create a response builder with the given status code and using default strategies for
	 * reading the body.
	 * @param statusCode the status code
	 * @return the created builder
	 */
	static Builder create(HttpStatus statusCode) {
		return create(statusCode, ExchangeStrategies.withDefaults());
	}

	/**
	 * Create a response builder with the given status code and strategies for reading the body.
	 * @param statusCode the status code
	 * @param strategies the strategies
	 * @return the created builder
	 */
	static Builder create(HttpStatus statusCode, ExchangeStrategies strategies) {
		return new DefaultClientResponseBuilder(strategies).statusCode(statusCode);
	}

	/**
	 * Create a response builder with the given status code and message body readers.
	 * @param statusCode the status code
	 * @param messageReaders the message readers
	 * @return the created builder
	 */
	static Builder create(HttpStatus statusCode, List<HttpMessageReader<?>> messageReaders) {
		return create(statusCode, new ExchangeStrategies() {
			@Override
			public List<HttpMessageReader<?>> messageReaders() {
				return messageReaders;
			}
			@Override
			public List<HttpMessageWriter<?>> messageWriters() {
				// not used in the response
				return Collections.emptyList();
			}
		});
	}


	/**
	 * Represents the headers of the HTTP response.
	 * @see ClientResponse#headers()
	 */
	interface Headers {

		/**
		 * Return the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 */
		OptionalLong contentLength();

		/**
		 * Return the {@linkplain MediaType media type} of the body, as specified
		 * by the {@code Content-Type} header.
		 */
		Optional<MediaType> contentType();

		/**
		 * Return the header value(s), if any, for the header of the given name.
		 * <p>Return an empty list if no header values are found.
		 * @param headerName the header name
		 */
		List<String> header(String headerName);

		/**
		 * Return the headers as a {@link HttpHeaders} instance.
		 */
		HttpHeaders asHttpHeaders();
	}


	/**
	 * Defines a builder for a response.
	 */
	interface Builder {

		/**
		 * Set the status code of the response.
		 * @param statusCode the new status code.
		 * @return this builder
		 */
		Builder statusCode(HttpStatus statusCode);

		/**
		 * Add the given header value(s) under the given name.
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * Manipulate this response's headers with the given consumer. The
		 * headers provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
		 * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
		 * {@link HttpHeaders} methods.
		 * @param headersConsumer a function that consumes the {@code HttpHeaders}
		 * @return this builder
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Add a cookie with the given name and value(s).
		 * @param name the cookie name
		 * @param values the cookie value(s)
		 * @return this builder
		 */
		Builder cookie(String name, String... values);

		/**
		 * Manipulate this response's cookies with the given consumer. The
		 * map provided to the consumer is "live", so that the consumer can be used to
		 * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing header values,
		 * {@linkplain MultiValueMap#remove(Object) remove} values, or use any of the other
		 * {@link MultiValueMap} methods.
		 * @param cookiesConsumer a function that consumes the cookies map
		 * @return this builder
		 */
		Builder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer);

		/**
		 * Set the body of the response. Calling this methods will
		 * {@linkplain org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer) release}
		 * the existing body of the builder.
		 * @param body the new body.
		 * @return this builder
		 */
		Builder body(Flux<DataBuffer> body);

		/**
		 * Set the body of the response to the UTF-8 encoded bytes of the given string.
		 * Calling this methods will
		 * {@linkplain org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer) release}
		 * the existing body of the builder.
		 * @param body the new body.
		 * @return this builder
		 */
		Builder body(String body);

		/**
		 * Build the response.
		 */
		ClientResponse build();
	}

}
