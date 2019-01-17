/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;

/**
 * Default implementation of {@link ClientResponse}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
class DefaultClientResponse implements ClientResponse {

	private final ClientHttpResponse response;

	private final Headers headers;

	private final ExchangeStrategies strategies;

	private final String logPrefix;


	public DefaultClientResponse(ClientHttpResponse response, ExchangeStrategies strategies, String logPrefix) {
		this.response = response;
		this.strategies = strategies;
		this.headers = new DefaultHeaders();
		this.logPrefix = logPrefix;
	}


	@Override
	public ExchangeStrategies strategies() {
		return this.strategies;
	}

	@Override
	public HttpStatus statusCode() {
		return this.response.getStatusCode();
	}

	@Override
	public int rawStatusCode() {
		return this.response.getRawStatusCode();
	}

	@Override
	public Headers headers() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, ResponseCookie> cookies() {
		return this.response.getCookies();
	}

	@Override
	public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
		return extractor.extract(this.response, new BodyExtractor.Context() {
			@Override
			public List<HttpMessageReader<?>> messageReaders() {
				return strategies.messageReaders();
			}
			@Override
			public Optional<ServerHttpResponse> serverResponse() {
				return Optional.empty();
			}
			@Override
			public Map<String, Object> hints() {
				return Hints.from(Hints.LOG_PREFIX_HINT, logPrefix);
			}
		});
	}

	@Override
	public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
		return body(BodyExtractors.toMono(elementClass));
	}

	@Override
	public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference) {
		return body(BodyExtractors.toMono(typeReference));
	}

	@Override
	public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
		return body(BodyExtractors.toFlux(elementClass));
	}

	@Override
	public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference) {
		return body(BodyExtractors.toFlux(typeReference));
	}

	@Override
	public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyType) {
		return toEntityInternal(bodyToMono(bodyType));
	}

	@Override
	public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> typeReference) {
		return toEntityInternal(bodyToMono(typeReference));
	}

	private <T> Mono<ResponseEntity<T>> toEntityInternal(Mono<T> bodyMono) {
		HttpHeaders headers = headers().asHttpHeaders();
		int status = rawStatusCode();
		return bodyMono
				.map(body -> createEntity(body, headers, status))
				.switchIfEmpty(Mono.defer(
						() -> Mono.just(createEntity(headers, status))));
	}

	@Override
	public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> responseType) {
		return toEntityListInternal(bodyToFlux(responseType));
	}

	@Override
	public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> typeReference) {
		return toEntityListInternal(bodyToFlux(typeReference));
	}

	private <T> Mono<ResponseEntity<List<T>>> toEntityListInternal(Flux<T> bodyFlux) {
		HttpHeaders headers = headers().asHttpHeaders();
		int status = rawStatusCode();
		return bodyFlux
				.collectList()
				.map(body -> createEntity(body, headers, status));
	}

	private <T> ResponseEntity<T> createEntity(HttpHeaders headers, int status) {
		HttpStatus resolvedStatus = HttpStatus.resolve(status);
		return resolvedStatus != null
				? new ResponseEntity<>(headers, resolvedStatus)
				: ResponseEntity.status(status).headers(headers).build();
	}

	private <T> ResponseEntity<T> createEntity(T body, HttpHeaders headers, int status) {
		HttpStatus resolvedStatus = HttpStatus.resolve(status);
		return resolvedStatus != null
				? new ResponseEntity<>(body, headers, resolvedStatus)
				: ResponseEntity.status(status).headers(headers).body(body);
	}


	private class DefaultHeaders implements Headers {

		private HttpHeaders delegate() {
			return response.getHeaders();
		}

		@Override
		public OptionalLong contentLength() {
			return toOptionalLong(delegate().getContentLength());
		}

		@Override
		public Optional<MediaType> contentType() {
			return Optional.ofNullable(delegate().getContentType());
		}

		@Override
		public List<String> header(String headerName) {
			List<String> headerValues = delegate().get(headerName);
			return (headerValues != null ? headerValues : Collections.emptyList());
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return HttpHeaders.readOnlyHttpHeaders(delegate());
		}

		private OptionalLong toOptionalLong(long value) {
			return (value != -1 ? OptionalLong.of(value) : OptionalLong.empty());
		}
	}

}
