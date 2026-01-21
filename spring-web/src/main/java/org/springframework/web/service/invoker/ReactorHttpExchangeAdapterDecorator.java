/*
 * Copyright 2002-present the original author or authors.
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

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * {@link ReactorHttpExchangeAdapter} that wraps and delegates to another adapter instance.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class ReactorHttpExchangeAdapterDecorator extends HttpExchangeAdapterDecorator
		implements ReactorHttpExchangeAdapter {


	public ReactorHttpExchangeAdapterDecorator(HttpExchangeAdapter delegate) {
		super(delegate);
	}


	/**
	 * Return the wrapped delegate {@code HttpExchangeAdapter}.
	 */
	@Override
	public ReactorHttpExchangeAdapter getHttpExchangeAdapter() {
		return (ReactorHttpExchangeAdapter) super.getHttpExchangeAdapter();
	}


	@Override
	public boolean supportsRequestAttributes() {
		return getHttpExchangeAdapter().supportsRequestAttributes();
	}

	@Override
	public void exchange(HttpRequestValues requestValues) {
		getHttpExchangeAdapter().exchange(requestValues);
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
		return getHttpExchangeAdapter().exchangeForHeaders(requestValues);
	}

	@Override
	public <T> @Nullable T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return getHttpExchangeAdapter().exchangeForBody(requestValues, bodyType);
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
		return getHttpExchangeAdapter().exchangeForBodilessEntity(requestValues);
	}

	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return getHttpExchangeAdapter().exchangeForEntity(requestValues, bodyType);
	}

	@Override
	public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
		return getHttpExchangeAdapter().getReactiveAdapterRegistry();
	}

	@Override
	public @Nullable Duration getBlockTimeout() {
		return getHttpExchangeAdapter().getBlockTimeout();
	}

	@Override
	public Mono<Void> exchangeForMono(HttpRequestValues requestValues) {
		return getHttpExchangeAdapter().exchangeForMono(requestValues);
	}

	@Override
	public Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues requestValues) {
		return getHttpExchangeAdapter().exchangeForHeadersMono(requestValues);
	}

	@Override
	public <T> Mono<T> exchangeForBodyMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return getHttpExchangeAdapter().exchangeForBodyMono(requestValues, bodyType);
	}

	@Override
	public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		return getHttpExchangeAdapter().exchangeForBodyFlux(requestValues, bodyType);
	}

	@Override
	public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues values) {
		return getHttpExchangeAdapter().exchangeForBodilessEntityMono(values);
	}

	@Override
	public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		return getHttpExchangeAdapter().exchangeForEntityMono(values, bodyType);
	}

	@Override
	public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		return getHttpExchangeAdapter().exchangeForEntityFlux(values, bodyType);
	}

}
