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
import java.util.Optional;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.web.client.reactive.ResponseExtractor;

import reactor.core.converter.RxJava1ObservableConverter;
import reactor.core.converter.RxJava1SingleConverter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.Single;

/**
 * Static factory methods for {@link ResponseExtractor}
 * based on the {@link Observable} and {@link Single} API.
 *
 * @author Brian Clozel
 */
public class 	RxJava1ResponseExtractors {

	/**
	 * Extract the response body and decode it, returning it as a {@code Single<T>}
	 */
	@SuppressWarnings("unchecked")
	public static <T> ResponseExtractor<Single<T>> body(Class<T> sourceClass) {

		ResolvableType resolvableType = ResolvableType.forClass(sourceClass);
		return (clientResponse, messageConverters) -> (Single<T>) RxJava1SingleConverter
				.fromPublisher(clientResponse
						.flatMap(resp -> decodeResponseBody(resp, resolvableType, messageConverters)).next());
	}

	/**
	 * Extract the response body and decode it, returning it as an {@code Observable<T>}
	 */
	public static <T> ResponseExtractor<Observable<T>> bodyStream(Class<T> sourceClass) {

		ResolvableType resolvableType = ResolvableType.forClass(sourceClass);
		return (clientResponse, messageConverters) -> RxJava1ObservableConverter
				.fromPublisher(clientResponse
						.flatMap(resp -> decodeResponseBody(resp, resolvableType, messageConverters)));
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity}
	 * with its body decoded as a single type {@code T}
	 */
	@SuppressWarnings("unchecked")
	public static <T> ResponseExtractor<Single<ResponseEntity<T>>> response(Class<T> sourceClass) {

		ResolvableType resolvableType = ResolvableType.forClass(sourceClass);
		return (clientResponse, messageConverters) ->
				RxJava1SingleConverter.fromPublisher(clientResponse
						.then(response ->
								Mono.when(
										decodeResponseBody(response, resolvableType, messageConverters).next(),
										Mono.just(response.getHeaders()),
										Mono.just(response.getStatusCode())))
						.map(tuple ->
								new ResponseEntity<>((T) tuple.getT1(), tuple.getT2(), tuple.getT3())));
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity}
	 * with its body decoded as an {@code Observable<T>}
	 */
	public static <T> ResponseExtractor<Single<ResponseEntity<Observable<T>>>> responseStream(Class<T> sourceClass) {
		ResolvableType resolvableType = ResolvableType.forClass(sourceClass);
		return (clientResponse, messageConverters) -> RxJava1SingleConverter.fromPublisher(clientResponse
				.map(response -> new ResponseEntity<>(
						RxJava1ObservableConverter
								.fromPublisher(decodeResponseBody(response, resolvableType, messageConverters)),
						response.getHeaders(),
						response.getStatusCode())));
	}

	/**
	 * Extract the response headers as an {@code HttpHeaders} instance
	 */
	public static ResponseExtractor<Single<HttpHeaders>> headers() {
		return (clientResponse, messageConverters) -> RxJava1SingleConverter
				.fromPublisher(clientResponse.map(resp -> resp.getHeaders()));
	}

	@SuppressWarnings("unchecked")
	protected static <T> Flux<T> decodeResponseBody(ClientHttpResponse response, ResolvableType responseType,
			List<HttpMessageConverter<?>> messageConverters) {

		MediaType contentType = response.getHeaders().getContentType();
		Optional<HttpMessageConverter<?>> converter = resolveConverter(messageConverters, responseType, contentType);
		if (!converter.isPresent()) {
			return Flux.error(new IllegalStateException("Could not decode response body of type '" + contentType +
					"' with target type '" + responseType.toString() + "'"));
		}
		return (Flux<T>) converter.get().read(responseType, response);
	}


	protected static Optional<HttpMessageConverter<?>> resolveConverter(List<HttpMessageConverter<?>> messageConverters,
			ResolvableType type, MediaType mediaType) {
		return messageConverters.stream().filter(e -> e.canRead(type, mediaType)).findFirst();
	}
}