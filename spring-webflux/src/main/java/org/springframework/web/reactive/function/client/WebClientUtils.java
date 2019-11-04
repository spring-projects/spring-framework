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

import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

/**
 * Internal methods shared between {@link DefaultWebClient} and {@link DefaultClientResponse}.
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
abstract class WebClientUtils {

	/**
	 * Create a delayed {@link ResponseEntity} from the given response and body.
	 */
	public static <T> Mono<ResponseEntity<T>> toEntity(ClientResponse response, Mono<T> bodyMono) {
		return Mono.defer(() -> {
			HttpHeaders headers = response.headers().asHttpHeaders();
			int status = response.rawStatusCode();
			return bodyMono
					.map(body -> createEntity(body, headers, status))
					.switchIfEmpty(Mono.fromCallable( () -> createEntity(null, headers, status)));
		});
	}

	/**
	 * Create a delayed {@link ResponseEntity} list from the given response and body.
	 */
	public static <T> Mono<ResponseEntity<List<T>>> toEntityList(ClientResponse response, Publisher<T> body) {
		return Mono.defer(() -> {
			HttpHeaders headers = response.headers().asHttpHeaders();
			int status = response.rawStatusCode();
			return Flux.from(body)
					.collectList()
					.map(list -> createEntity(list, headers, status));
		});
	}

	public static <T> ResponseEntity<T> createEntity(@Nullable T body, HttpHeaders headers, int status) {
		HttpStatus resolvedStatus = HttpStatus.resolve(status);
		return resolvedStatus != null
				? new ResponseEntity<>(body, headers, resolvedStatus)
				: ResponseEntity.status(status).headers(headers).body(body);
	}

}
