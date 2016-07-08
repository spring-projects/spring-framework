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

import java.util.List;
import java.util.Optional;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.converter.reactive.HttpMessageConverter;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Static factory methods for {@link ResponseExtractor} based on the {@link Flux} and
 * {@link Mono} API.
 *
 * @author Brian Clozel
 */
public class ResponseExtractors {

	private static final Object EMPTY_BODY = new Object();

	/**
	 * Extract the response body and decode it, returning it as a {@code Mono<T>}
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	public static <T> ResponseExtractor<Mono<T>> body(ResolvableType bodyType) {
		// noinspection unchecked
		return (clientResponse, messageConverters) -> (Mono<T>) clientResponse
				.flatMap(resp -> decodeResponseBody(resp, bodyType,
						messageConverters))
				.next();
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Mono<T>}
	 */
	public static <T> ResponseExtractor<Mono<T>> body(Class<T> sourceClass) {
		ResolvableType bodyType = ResolvableType.forClass(sourceClass);
		return body(bodyType);
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Flux<T>}
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	public static <T> ResponseExtractor<Flux<T>> bodyStream(ResolvableType bodyType) {
		return (clientResponse, messageConverters) -> clientResponse
				.flatMap(resp -> decodeResponseBody(resp, bodyType, messageConverters));
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Flux<T>}
	 */
	public static <T> ResponseExtractor<Flux<T>> bodyStream(Class<T> sourceClass) {
		ResolvableType bodyType = ResolvableType.forClass(sourceClass);
		return bodyStream(bodyType);
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity} with its body decoded as
	 * a single type {@code T}
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	public static <T> ResponseExtractor<Mono<ResponseEntity<T>>> response(
			ResolvableType bodyType) {
		return (clientResponse, messageConverters) -> clientResponse.then(response -> {
			return Mono.when(
					decodeResponseBody(response, bodyType,
							messageConverters).next().defaultIfEmpty(
							EMPTY_BODY),
					Mono.just(response.getHeaders()),
					Mono.just(response.getStatusCode()));
		}).map(tuple -> {
			Object body = (tuple.getT1() != EMPTY_BODY ? tuple.getT1() : null);
			// noinspection unchecked
			return new ResponseEntity<>((T) body, tuple.getT2(), tuple.getT3());
		});
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity} with its body decoded as
	 * a single type {@code T}
	 */
	public static <T> ResponseExtractor<Mono<ResponseEntity<T>>> response(
			Class<T> bodyClass) {
		ResolvableType bodyType = ResolvableType.forClass(bodyClass);
		return response(bodyType);
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity} with its body decoded as
	 * a {@code Flux<T>}
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	public static <T> ResponseExtractor<Mono<ResponseEntity<Flux<T>>>> responseStream(
			ResolvableType type) {
		return (clientResponse, messageConverters) -> clientResponse
				.map(response -> new ResponseEntity<>(
						decodeResponseBody(response, type,
								messageConverters),
						response.getHeaders(), response.getStatusCode()));
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity} with its body decoded as
	 * a {@code Flux<T>}
	 */
	public static <T> ResponseExtractor<Mono<ResponseEntity<Flux<T>>>> responseStream(
			Class<T> sourceClass) {
		ResolvableType resolvableType = ResolvableType.forClass(sourceClass);
		return responseStream(resolvableType);
	}

	/**
	 * Extract the response headers as an {@code HttpHeaders} instance
	 */
	public static ResponseExtractor<Mono<HttpHeaders>> headers() {
		return (clientResponse, messageConverters) -> clientResponse.map(resp -> resp.getHeaders());
	}

	protected static <T> Flux<T> decodeResponseBody(ClientHttpResponse response,
			ResolvableType responseType,
			List<HttpMessageConverter<?>> messageConverters) {

		MediaType contentType = response.getHeaders().getContentType();
		Optional<HttpMessageConverter<?>> converter = resolveConverter(messageConverters,
				responseType, contentType);
		if (!converter.isPresent()) {
			return Flux.error(new IllegalStateException(
					"Could not decode response body of type '" + contentType
							+ "' with target type '" + responseType.toString() + "'"));
		}
		// noinspection unchecked
		return (Flux<T>) converter.get().read(responseType, response);
	}

	protected static Optional<HttpMessageConverter<?>> resolveConverter(
			List<HttpMessageConverter<?>> messageConverters, ResolvableType type,
			MediaType mediaType) {
		return messageConverters.stream().filter(e -> e.canRead(type, mediaType))
				.findFirst();
	}
}