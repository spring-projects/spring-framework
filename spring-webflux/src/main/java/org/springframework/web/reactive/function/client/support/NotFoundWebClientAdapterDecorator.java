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

package org.springframework.web.reactive.function.client.support;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.ReactorHttpExchangeAdapterDecorator;

/**
 * {@code HttpExchangeAdapterDecorator} that suppresses the
 * {@link WebClientResponseException.NotFound} exception resulting from 404
 * responses and returns a {@code ResponseEntity} with the status set to
 * {@link org.springframework.http.HttpStatus#NOT_FOUND} status, or an empty
 * {@code Mono} from {@link #exchangeForBodyMono}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class NotFoundWebClientAdapterDecorator extends ReactorHttpExchangeAdapterDecorator {


	public NotFoundWebClientAdapterDecorator(HttpExchangeAdapter delegate) {
		super(delegate);
	}


	@Override
	public <T> @Nullable T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		try {
			return super.exchangeForBody(values, bodyType);
		}
		catch (WebClientResponseException.NotFound ex) {
			return null;
		}
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues values) {
		try {
			return super.exchangeForBodilessEntity(values);
		}
		catch (WebClientResponseException.NotFound ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		try {
			return super.exchangeForEntity(values, bodyType);
		}
		catch (WebClientResponseException.NotFound ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@Override
	public <T> Mono<T> exchangeForBodyMono(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		return super.exchangeForBodyMono(values, bodyType).onErrorResume(
				WebClientResponseException.NotFound.class, ex -> Mono.empty());
	}

	@Override
	public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
		return super.exchangeForBodyFlux(values, bodyType).onErrorResume(
				WebClientResponseException.NotFound.class, ex -> Flux.empty());
	}

	@Override
	public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues values) {
		return super.exchangeForBodilessEntityMono(values).onErrorResume(
				WebClientResponseException.NotFound.class, ex -> Mono.just(ResponseEntity.notFound().build()));
	}

	@Override
	public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(
			HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {

		return super.exchangeForEntityMono(values, bodyType).onErrorResume(
				WebClientResponseException.NotFound.class, ex -> Mono.just(ResponseEntity.notFound().build()));
	}

	@Override
	public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(
			HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {

		return super.exchangeForEntityFlux(values, bodyType).onErrorResume(
				WebClientResponseException.NotFound.class, ex -> Mono.just(ResponseEntity.notFound().build()));
	}

}
