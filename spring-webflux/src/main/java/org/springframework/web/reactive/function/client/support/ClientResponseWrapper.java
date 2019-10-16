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

package org.springframework.web.reactive.function.client.support;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Implementation of the {@link ClientResponse} interface that can be subclassed
 * to adapt the request in a
 * {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction exchange filter function}.
 * All methods default to calling through to the wrapped request.
 *
 * @author Arjen Poutsma
 * @since 5.0.5
 */
public class ClientResponseWrapper implements ClientResponse {

	private final ClientResponse delegate;


	/**
	 * Create a new {@code ClientResponseWrapper} that wraps the given response.
	 * @param delegate the response to wrap
	 */
	public ClientResponseWrapper(ClientResponse delegate) {
		Assert.notNull(delegate, "Delegate is required");
		this.delegate = delegate;
	}


	/**
	 * Return the wrapped request.
	 */
	public ClientResponse response() {
		return this.delegate;
	}

	@Override
	public ExchangeStrategies strategies() {
		return this.delegate.strategies();
	}

	@Override
	public HttpStatus statusCode() {
		return this.delegate.statusCode();
	}

	@Override
	public int rawStatusCode() {
		return this.delegate.rawStatusCode();
	}

	@Override
	public Headers headers() {
		return this.delegate.headers();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> cookies() {
		return this.delegate.cookies();
	}

	@Override
	public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
		return this.delegate.body(extractor);
	}

	@Override
	public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
		return this.delegate.bodyToMono(elementClass);
	}

	@Override
	public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef) {
		return this.delegate.bodyToMono(elementTypeRef);
	}

	@Override
	public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
		return this.delegate.bodyToFlux(elementClass);
	}

	@Override
	public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef) {
		return this.delegate.bodyToFlux(elementTypeRef);
	}

	@Override
	public Mono<Void> releaseBody() {
		return this.delegate.releaseBody();
	}

	@Override
	public Mono<ResponseEntity<Void>> toBodilessEntity() {
		return this.delegate.toBodilessEntity();
	}

	@Override
	public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyType) {
		return this.delegate.toEntity(bodyType);
	}

	@Override
	public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference) {
		return this.delegate.toEntity(bodyTypeReference);
	}

	@Override
	public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass) {
		return this.delegate.toEntityList(elementClass);
	}

	@Override
	public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef) {
		return this.delegate.toEntityList(elementTypeRef);
	}

	@Override
	public Mono<WebClientResponseException> createException() {
		return this.delegate.createException();
	}

	/**
	 * Implementation of the {@code Headers} interface that can be subclassed
	  * to adapt the headers in a
	  * {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction exchange filter function}.
	  * All methods default to calling through to the wrapped request.
	 */
	public static class HeadersWrapper implements ClientResponse.Headers {

		private final Headers headers;


		/**
		 * Create a new {@code HeadersWrapper} that wraps the given request.
		 * @param headers the headers to wrap
		 */
		public HeadersWrapper(Headers headers) {
			this.headers = headers;
		}


		@Override
		public OptionalLong contentLength() {
			return this.headers.contentLength();
		}

		@Override
		public Optional<MediaType> contentType() {
			return this.headers.contentType();
		}

		@Override
		public List<String> header(String headerName) {
			return this.headers.header(headerName);
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return this.headers.asHttpHeaders();
		}
	}

}
