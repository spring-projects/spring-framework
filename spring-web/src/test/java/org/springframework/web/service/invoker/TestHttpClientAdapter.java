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

import java.util.Collections;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HttpClientAdapter} with stubbed responses.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings("unchecked")
class TestHttpClientAdapter implements HttpClientAdapter, TestAdapter {

	@Nullable
	private String invokedForReturnMethodReference;

	@Nullable
	private HttpRequestValues requestValues;

	@Nullable
	private ParameterizedTypeReference<?> bodyType;

	@Override
	public String getInvokedMethodReference() {
		assertThat(this.invokedForReturnMethodReference).isNotNull();
		return this.invokedForReturnMethodReference;
	}

	@Override
	public HttpRequestValues getRequestValues() {
		assertThat(this.requestValues).isNotNull();
		return this.requestValues;
	}

	@Override
	@Nullable
	public ParameterizedTypeReference<?> getBodyType() {
		return this.bodyType;
	}


	// HttpClientAdapter implementation

	@Override
	public Mono<Void> requestToVoid(HttpRequestValues requestValues) {
		saveInput("void", requestValues, null);
		return Mono.empty();
	}

	@Override
	public Mono<HttpHeaders> requestToHeaders(HttpRequestValues requestValues) {
		saveInput("headers", requestValues, null);
		return Mono.just(new HttpHeaders());
	}

	@Override
	public <T> Mono<T> requestToBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		saveInput("body", requestValues, bodyType);
		return  bodyType.getType().getTypeName().contains("List") ?
				(Mono<T>) Mono.just(Collections.singletonList(getInvokedMethodReference()))
				: (Mono<T>) Mono.just(getInvokedMethodReference());
	}

	@Override
	public <T> Flux<T> requestToBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		saveInput("bodyFlux", requestValues, bodyType);
		return (Flux<T>) Flux.just("request", "To", "Body", "Flux");
	}

	@Override
	public Mono<ResponseEntity<Void>> requestToBodilessEntity(HttpRequestValues requestValues) {
		saveInput("bodilessEntity", requestValues, null);
		return Mono.just(ResponseEntity.ok().build());
	}

	@Override
	public <T> Mono<ResponseEntity<T>> requestToEntity(
			HttpRequestValues requestValues, ParameterizedTypeReference<T> type) {

		saveInput("entity", requestValues, type);
		return Mono.just((ResponseEntity<T>) ResponseEntity.ok("entity"));
	}

	@Override
	public <T> Mono<ResponseEntity<Flux<T>>> requestToEntityFlux(
			HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

		saveInput("entityFlux", requestValues, bodyType);
		return Mono.just(ResponseEntity.ok((Flux<T>) Flux.just("request", "To", "Entity", "Flux")));
	}

	private <T> void saveInput(
			String reference, HttpRequestValues requestValues, @Nullable ParameterizedTypeReference<T> bodyType) {

		this.invokedForReturnMethodReference = reference;
		this.requestValues = requestValues;
		this.bodyType = bodyType;
	}

}
