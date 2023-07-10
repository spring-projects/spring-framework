/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.service.invoker;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Contract to abstract the underlying HTTP client and decouple it from the
 * {@linkplain HttpServiceProxyFactory#createClient(Class) HTTP service proxy}.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 * @since 6.0
 * @deprecated in favor of {@link ReactorHttpExchangeAdapter}
 */
@Deprecated(since = "6.1", forRemoval = true)
public interface HttpClientAdapter {

	/**
	 * Perform the given request, and release the response content, if any.
	 * @param requestValues the request to perform
	 * @return {@code Mono} that completes when the request is fully executed
	 * and the response content is released.
	 */
	Mono<Void> requestToVoid(HttpRequestValues requestValues);

	/**
	 * Perform the given request, release the response content, and return the
	 * response headers.
	 * @param requestValues the request to perform
	 * @return {@code Mono} that returns the response headers the request is
	 * fully executed and the response content released.
	 */
	Mono<HttpHeaders> requestToHeaders(HttpRequestValues requestValues);

	/**
	 * Perform the given request and decode the response content to the given type.
	 * @param requestValues the request to perform
	 * @param bodyType the target type to decode to
	 * @return {@code Mono} that returns the decoded response.
	 * @param <T> the type the response is decoded to
	 */
	<T> Mono<T> requestToBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

	/**
	 * Perform the given request and decode the response content to a stream with
	 * elements of the given type.
	 * @param requestValues the request to perform
	 * @param bodyType the target stream element type to decode to
	 * @return {@code Flux} with decoded stream elements.
	 * @param <T> the type the response is decoded to
	 */
	<T> Flux<T> requestToBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

	/**
	 * Variant of {@link #requestToVoid(HttpRequestValues)} with additional
	 * access to the response status and headers.
	 */
	Mono<ResponseEntity<Void>> requestToBodilessEntity(HttpRequestValues requestValues);

	/**
	 * Variant of {@link #requestToBody(HttpRequestValues, ParameterizedTypeReference)}
	 * with additional access to the response status and headers.
	 */
	<T> Mono<ResponseEntity<T>> requestToEntity(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);

	/**
	 * Variant of {@link #requestToBodyFlux(HttpRequestValues, ParameterizedTypeReference)}
	 * with additional access to the response status and headers.
	 */
	<T> Mono<ResponseEntity<Flux<T>>> requestToEntityFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType);


	/**
	 * Adapt this instance to {@link ReactorHttpExchangeAdapter}.
	 * @since 6.1
	 */
	default ReactorHttpExchangeAdapter asReactorExchangeAdapter() {

		return new AbstractReactorHttpExchangeAdapter() {

			@Override
			public boolean supportsRequestAttributes() {
				return true;
			}

			@Override
			public Mono<Void> exchangeForMono(HttpRequestValues values) {
				return HttpClientAdapter.this.requestToVoid(values);
			}

			@Override
			public Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues values) {
				return HttpClientAdapter.this.requestToHeaders(values);
			}

			@Override
			public <T> Mono<T> exchangeForBodyMono(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
				return HttpClientAdapter.this.requestToBody(values, bodyType);
			}

			@Override
			public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
				return HttpClientAdapter.this.requestToBodyFlux(values, bodyType);
			}

			@Override
			public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues values) {
				return HttpClientAdapter.this.requestToBodilessEntity(values);
			}

			@Override
			public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(
					HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

				return HttpClientAdapter.this.requestToEntity(requestValues, bodyType);
			}

			@Override
			public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(
					HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

				return HttpClientAdapter.this.requestToEntityFlux(requestValues, bodyType);
			}
		};
	}

}
