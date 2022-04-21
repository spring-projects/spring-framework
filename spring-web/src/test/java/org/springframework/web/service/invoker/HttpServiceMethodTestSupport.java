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

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contains utility methods for {@link HttpServiceMethod} tests.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
public class HttpServiceMethodTestSupport {

	private final TestHttpClientAdapter clientAdapter;

	protected HttpServiceMethodTestSupport() {
		clientAdapter = new TestHttpClientAdapter();
	}

	protected <S> S createService(Class<S> serviceType) {
		return createService(serviceType, Collections.emptyList());
	}

	protected <S> S createService(Class<S> serviceType,
			List<HttpServiceMethodArgumentResolver> argumentResolvers) {
		HttpServiceProxyFactory factory = new HttpServiceProxyFactory(
				argumentResolvers, this.clientAdapter, ReactiveAdapterRegistry.getSharedInstance(),
				Duration.ofSeconds(5));

		return factory.createService(serviceType);
	}

	protected HttpRequestDefinition getRequestDefinition() {
		return this.clientAdapter.getRequestDefinition();
	}


	protected TestHttpClientAdapter getClientAdapter() {
		return this.clientAdapter;
	}

	protected static class TestHttpClientAdapter implements HttpClientAdapter {

		@Nullable
		private String methodName;

		@Nullable
		private HttpRequestDefinition requestDefinition;

		@Nullable
		private ParameterizedTypeReference<?> bodyType;


		String getMethodName() {
			assertThat(this.methodName).isNotNull();
			return this.methodName;
		}

		HttpRequestDefinition getRequestDefinition() {
			assertThat(this.requestDefinition).isNotNull();
			return this.requestDefinition;
		}

		@Nullable
		public ParameterizedTypeReference<?> getBodyType() {
			return this.bodyType;
		}


		@Override
		public Mono<Void> requestToVoid(HttpRequestDefinition def) {
			saveInput("requestToVoid", def, null);
			return Mono.empty();
		}

		@Override
		public Mono<HttpHeaders> requestToHeaders(HttpRequestDefinition def) {
			saveInput("requestToHeaders", def, null);
			return Mono.just(new HttpHeaders());
		}

		@Override
		public <T> Mono<T> requestToBody(HttpRequestDefinition def,
				ParameterizedTypeReference<T> bodyType) {
			saveInput("requestToBody", def, bodyType);
			return (Mono<T>) Mono.just(getMethodName());
		}

		@Override
		public <T> Flux<T> requestToBodyFlux(HttpRequestDefinition def,
				ParameterizedTypeReference<T> bodyType) {
			saveInput("requestToBodyFlux", def, bodyType);
			return (Flux<T>) Flux.just("request", "To", "Body", "Flux");
		}

		@Override
		public Mono<ResponseEntity<Void>> requestToBodilessEntity(HttpRequestDefinition def) {
			saveInput("requestToBodilessEntity", def, null);
			return Mono.just(ResponseEntity.ok().build());
		}

		@Override
		public <T> Mono<ResponseEntity<T>> requestToEntity(HttpRequestDefinition def,
				ParameterizedTypeReference<T> bodyType) {
			saveInput("requestToEntity", def, bodyType);
			return Mono.just((ResponseEntity<T>) ResponseEntity.ok("requestToEntity"));
		}

		@Override
		public <T> Mono<ResponseEntity<Flux<T>>> requestToEntityFlux(HttpRequestDefinition def,
				ParameterizedTypeReference<T> bodyType) {
			saveInput("requestToEntityFlux", def, bodyType);
			return Mono.just(ResponseEntity.ok((Flux<T>) Flux.just("request", "To", "Entity", "Flux")));
		}

		private <T> void saveInput(
				String methodName, HttpRequestDefinition definition,
				@Nullable ParameterizedTypeReference<T> bodyType) {

			this.methodName = methodName;
			this.requestDefinition = definition;
			this.bodyType = bodyType;
		}

	}

}
