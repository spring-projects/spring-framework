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

import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;

/**
 * Static factory methods for {@link ResponseExtractor} and {@link BodyExtractor},
 * based on the {@link Flux} and {@link Mono} APIs.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public class ResponseExtractors {

	private static final Object EMPTY_BODY = new Object();

	/**
	 * Extract the response body and decode it, returning it as a {@code Mono<T>}.
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	@SuppressWarnings("unchecked")
	public static <T> ResponseExtractor<Mono<T>> body(ResolvableType bodyType) {
		return (clientResponse, webClientConfig) -> (Mono<T>) clientResponse
				.doOnNext(response -> webClientConfig.getResponseErrorHandler()
						.handleError(response, webClientConfig.getMessageReaders()))
				.flatMap(resp -> decodeResponseBodyAsMono(resp, bodyType,
						webClientConfig.getMessageReaders()))
				.next();
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Mono<T>}.
	 */
	public static <T> ResponseExtractor<Mono<T>> body(Class<T> sourceClass) {
		ResolvableType bodyType = ResolvableType.forClass(sourceClass);
		return body(bodyType);
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Mono<T>}.
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	public static <T> BodyExtractor<Mono<T>> as(ResolvableType bodyType) {
		return (clientResponse, messageConverters) ->
				decodeResponseBodyAsMono(clientResponse, bodyType, messageConverters);
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Mono<T>}
	 */
	public static <T> BodyExtractor<Mono<T>> as(Class<T> sourceClass) {
		ResolvableType bodyType = ResolvableType.forClass(sourceClass);
		return as(bodyType);
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Flux<T>}.
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	public static <T> ResponseExtractor<Flux<T>> bodyStream(ResolvableType bodyType) {
		return (clientResponse, webClientConfig) -> clientResponse
				.doOnNext(response -> webClientConfig.getResponseErrorHandler()
						.handleError(response, webClientConfig.getMessageReaders()))
				.flatMap(resp -> decodeResponseBody(resp, bodyType, webClientConfig.getMessageReaders()));
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Flux<T>}.
	 */
	public static <T> ResponseExtractor<Flux<T>> bodyStream(Class<T> sourceClass) {
		ResolvableType bodyType = ResolvableType.forClass(sourceClass);
		return bodyStream(bodyType);
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Flux<T>}
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	@SuppressWarnings("unchecked")
	public static <T> BodyExtractor<Flux<T>> asStream(ResolvableType bodyType) {
		return (clientResponse, messageConverters) ->
				(Flux<T>) decodeResponseBody(clientResponse, bodyType, messageConverters);
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Flux<T>}
	 */
	public static <T> BodyExtractor<Flux<T>> asStream(Class<T> sourceClass) {
		ResolvableType bodyType = ResolvableType.forClass(sourceClass);
		return asStream(bodyType);
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity} with its body decoded as
	 * a single type {@code T}.
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	@SuppressWarnings("unchecked")
	public static <T> ResponseExtractor<Mono<ResponseEntity<T>>> response(ResolvableType bodyType) {

		return (clientResponse, webClientConfig) -> clientResponse.then(response ->
				Mono.when(
						decodeResponseBodyAsMono(response, bodyType,
								webClientConfig.getMessageReaders()).defaultIfEmpty(EMPTY_BODY),
						Mono.just(response.getHeaders()),
						Mono.just(response.getStatusCode()))
		).map(tuple -> {
			Object body = (tuple.getT1() != EMPTY_BODY ? tuple.getT1() : null);
			return new ResponseEntity<>((T) body, tuple.getT2(), tuple.getT3());
		});
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity} with its body decoded as
	 * a single type {@code T}.
	 */
	public static <T> ResponseExtractor<Mono<ResponseEntity<T>>> response(Class<T> bodyClass) {
		ResolvableType bodyType = ResolvableType.forClass(bodyClass);
		return response(bodyType);
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity} with its body decoded as
	 * a {@code Flux<T>}.
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	public static <T> ResponseExtractor<Mono<ResponseEntity<Flux<T>>>> responseStream(ResolvableType type) {
		return (clientResponse, webClientConfig) -> clientResponse
				.map(response -> new ResponseEntity<>(
						// ResponseExtractors.<T> is required for Eclipse JDT.
						ResponseExtractors.<T> decodeResponseBody(response, type, webClientConfig.getMessageReaders()),
						response.getHeaders(), response.getStatusCode()));
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity} with its body decoded as
	 * a {@code Flux<T>}.
	 */
	public static <T> ResponseExtractor<Mono<ResponseEntity<Flux<T>>>> responseStream(Class<T> sourceClass) {
		ResolvableType resolvableType = ResolvableType.forClass(sourceClass);
		return responseStream(resolvableType);
	}

	/**
	 * Extract the response headers as an {@code HttpHeaders} instance.
	 */
	public static ResponseExtractor<Mono<HttpHeaders>> headers() {
		return (clientResponse, webClientConfig) -> clientResponse.map(resp -> resp.getHeaders());
	}

	@SuppressWarnings("unchecked")
	protected static <T> Flux<T> decodeResponseBody(ClientHttpResponse response,
			ResolvableType responseType, List<HttpMessageReader<?>> messageReaders) {

		MediaType contentType = response.getHeaders().getContentType();
		HttpMessageReader<?> reader = resolveMessageReader(messageReaders, responseType, contentType);
		return (Flux<T>) reader.read(responseType, response, Collections.emptyMap());
	}

	@SuppressWarnings("unchecked")
	protected static <T> Mono<T> decodeResponseBodyAsMono(ClientHttpResponse response,
			ResolvableType responseType, List<HttpMessageReader<?>> messageReaders) {

		MediaType contentType = response.getHeaders().getContentType();
		HttpMessageReader<?> reader = resolveMessageReader(messageReaders, responseType, contentType);
		return (Mono<T>) reader.readMono(responseType, response, Collections.emptyMap());
	}

	protected static HttpMessageReader<?> resolveMessageReader(List<HttpMessageReader<?>> messageReaders,
			ResolvableType responseType, MediaType contentType) {

		return messageReaders.stream()
				.filter(e -> e.canRead(responseType, contentType, Collections.emptyMap()))
				.findFirst()
				.orElseThrow(() ->
						new WebClientException(
								"Could not decode response body of type '" + contentType
										+ "' with target type '" + responseType.toString() + "'"));
	}

}
