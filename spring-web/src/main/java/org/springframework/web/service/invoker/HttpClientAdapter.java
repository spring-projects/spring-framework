/*
 * Copyright 2002-2022 the original author or authors.
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
 * @since 6.0
 */
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

}
