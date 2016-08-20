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

package org.springframework.web.client.reactive.support;

import java.util.List;

import reactor.adapter.RxJava1Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.Single;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.web.client.reactive.BodyExtractor;
import org.springframework.web.client.reactive.ResponseExtractor;
import org.springframework.web.client.reactive.WebClientException;

/**
 * Static factory methods for {@link ResponseExtractor} and {@link BodyExtractor},
 * based on the {@link Observable} and {@link Single} APIs.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public class RxJava1ResponseExtractors {

	/**
	 * Extract the response body and decode it, returning it as a {@code Single<T>}.
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	@SuppressWarnings("unchecked")
	public static <T> ResponseExtractor<Single<T>> body(ResolvableType bodyType) {

		return (clientResponse, webClientConfig) -> (Single<T>) RxJava1Adapter
				.publisherToSingle(clientResponse
						.doOnNext(response -> webClientConfig.getResponseErrorHandler()
								.handleError(response, webClientConfig.getMessageReaders()))
						.flatMap(resp -> decodeResponseBodyAsMono(resp, bodyType, webClientConfig.getMessageReaders())));
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Single<T>}.
	 */
	public static <T> ResponseExtractor<Single<T>> body(Class<T> sourceClass) {
		ResolvableType bodyType = ResolvableType.forClass(sourceClass);
		return body(bodyType);
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Single<T>}.
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	@SuppressWarnings("unchecked")
	public static <T> BodyExtractor<Single<T>> as(ResolvableType bodyType) {
		return (clientResponse, messageConverters) ->
				(Single<T>) RxJava1Adapter.publisherToSingle(
						decodeResponseBodyAsMono(clientResponse, bodyType, messageConverters));
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Single<T>}
	 */
	public static <T> BodyExtractor<Single<T>> as(Class<T> sourceClass) {
		ResolvableType bodyType = ResolvableType.forClass(sourceClass);
		return as(bodyType);
	}

	/**
	 * Extract the response body and decode it, returning it as an {@code Observable<T>}
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	public static <T> ResponseExtractor<Observable<T>> bodyStream(ResolvableType bodyType) {

		return (clientResponse, webClientConfig) -> RxJava1Adapter
				.publisherToObservable(clientResponse
						.doOnNext(response -> webClientConfig.getResponseErrorHandler()
								.handleError(response, webClientConfig.getMessageReaders()))
						.flatMap(resp -> decodeResponseBody(resp, bodyType, webClientConfig.getMessageReaders())));
	}

	/**
	 * Extract the response body and decode it, returning it as an {@code Observable<T>}.
	 */
	public static <T> ResponseExtractor<Observable<T>> bodyStream(Class<T> sourceClass) {

		ResolvableType bodyType = ResolvableType.forClass(sourceClass);
		return bodyStream(bodyType);
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Observable<T>}.
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	@SuppressWarnings("unchecked")
	public static <T> BodyExtractor<Observable<T>> asStream(ResolvableType bodyType) {
		return (clientResponse, messageConverters) ->
				(Observable<T>) RxJava1Adapter
						.publisherToObservable(decodeResponseBody(clientResponse, bodyType, messageConverters));
	}

	/**
	 * Extract the response body and decode it, returning it as a {@code Observable<T>}.
	 */
	public static <T> BodyExtractor<Observable<T>> asStream(Class<T> sourceClass) {
		ResolvableType bodyType = ResolvableType.forClass(sourceClass);
		return asStream(bodyType);
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity}
	 * with its body decoded as a single type {@code T}.
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	@SuppressWarnings("unchecked")
	public static <T> ResponseExtractor<Single<ResponseEntity<T>>> response(ResolvableType bodyType) {

		return (clientResponse, webClientConfig) ->
				RxJava1Adapter.publisherToSingle(clientResponse
						.then(response ->
								Mono.when(
										decodeResponseBody(response, bodyType, webClientConfig.getMessageReaders()).next(),
										Mono.just(response.getHeaders()),
										Mono.just(response.getStatusCode())))
						.map(tuple ->
							new ResponseEntity<>((T) tuple.getT1(), tuple.getT2(), tuple.getT3())));
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity}
	 * with its body decoded as a single type {@code T}.
	 */
	public static <T> ResponseExtractor<Single<ResponseEntity<T>>> response(Class<T> sourceClass) {

		ResolvableType bodyType = ResolvableType.forClass(sourceClass);
		return response(bodyType);
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity}
	 * with its body decoded as an {@code Observable<T>}.
	 */
	public static <T> ResponseExtractor<Single<ResponseEntity<Observable<T>>>> responseStream(Class<T> sourceClass) {
		ResolvableType resolvableType = ResolvableType.forClass(sourceClass);
		return responseStream(resolvableType);
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity}
	 * with its body decoded as an {@code Observable<T>}
	 * @see ResolvableType#forClassWithGenerics(Class, Class[])
	 */
	public static <T> ResponseExtractor<Single<ResponseEntity<Observable<T>>>> responseStream(ResolvableType bodyType) {
		return (clientResponse, webClientConfig) -> RxJava1Adapter.publisherToSingle(clientResponse
				.map(response -> new ResponseEntity<>(
								RxJava1Adapter
								.publisherToObservable(
										// RxJava1ResponseExtractors.<T> is required for Eclipse JDT.
										RxJava1ResponseExtractors.<T> decodeResponseBody(response, bodyType, webClientConfig.getMessageReaders())),
						response.getHeaders(),
						response.getStatusCode())));
	}

	/**
	 * Extract the response headers as an {@code HttpHeaders} instance.
	 */
	public static ResponseExtractor<Single<HttpHeaders>> headers() {
		return (clientResponse, messageConverters) -> RxJava1Adapter
				.publisherToSingle(clientResponse.map(resp -> resp.getHeaders()));
	}

	@SuppressWarnings("unchecked")
	protected static <T> Flux<T> decodeResponseBody(ClientHttpResponse response,
			ResolvableType responseType, List<HttpMessageReader<?>> messageReaders) {

		MediaType contentType = response.getHeaders().getContentType();
		HttpMessageReader<?> converter = resolveMessageReader(messageReaders, responseType, contentType);
		return (Flux<T>) converter.read(responseType, response);
	}

	@SuppressWarnings("unchecked")
	protected static <T> Mono<T> decodeResponseBodyAsMono(ClientHttpResponse response,
			ResolvableType responseType, List<HttpMessageReader<?>> messageReaders) {

		MediaType contentType = response.getHeaders().getContentType();
		HttpMessageReader<?> converter = resolveMessageReader(messageReaders, responseType, contentType);
		return (Mono<T>) converter.readMono(responseType, response);
	}

	protected static HttpMessageReader<?> resolveMessageReader(List<HttpMessageReader<?>> messageReaders,
			ResolvableType responseType, MediaType contentType) {

		return messageReaders.stream()
				.filter(e -> e.canRead(responseType, contentType))
				.findFirst()
				.orElseThrow(() ->
						new WebClientException(
								"Could not decode response body of type '" + contentType
										+ "' with target type '" + responseType.toString() + "'"));
	}

}
