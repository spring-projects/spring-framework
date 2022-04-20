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
 * Decouple an {@link HttpServiceProxyFactory#createService(Class) HTTP Service proxy}
 * from the underlying HTTP client.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public interface HttpClientAdapter {

	Mono<Void> requestToVoid(HttpRequestDefinition requestDefinition);

	Mono<HttpHeaders> requestToHeaders(HttpRequestDefinition requestDefinition);

	<T> Mono<T> requestToBody(HttpRequestDefinition requestDefinition, ParameterizedTypeReference<T> bodyType);

	<T> Flux<T> requestToBodyFlux(HttpRequestDefinition requestDefinition, ParameterizedTypeReference<T> bodyType);

	Mono<ResponseEntity<Void>> requestToBodilessEntity(HttpRequestDefinition requestDefinition);

	<T> Mono<ResponseEntity<T>> requestToEntity(HttpRequestDefinition requestDefinition, ParameterizedTypeReference<T> bodyType);

	<T> Mono<ResponseEntity<Flux<T>>> requestToEntityFlux(HttpRequestDefinition requestDefinition, ParameterizedTypeReference<T> bodyType);

}
